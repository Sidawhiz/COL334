/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <fstream>
#include "ns3/core-module.h"
#include "ns3/network-module.h"
#include "ns3/internet-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/applications-module.h"
#include "ns3/TcpNewRenoCSE.h"

using namespace ns3;

NS_LOG_COMPONENT_DEFINE ("SixthScriptExample");

// ===========================================================================
//
//         node 0                 node 1
//   +----------------+    +----------------+
//   |    ns-3 TCP    |    |    ns-3 TCP    |
//   +----------------+    +----------------+
//   |    10.1.1.1    |    |    10.1.1.2    |
//   +----------------+    +----------------+
//   | point-to-point |    | point-to-point |
//   +----------------+    +----------------+
//           |                     |
//           +---------------------+
//                5 Mbps, 2 ms
//
//
// We want to look at changes in the ns-3 TCP congestion window.  We need
// to crank up a flow and hook the CongestionWindow attribute on the socket
// of the sender.  Normally one would use an on-off application to generate a
// flow, but this has a couple of problems.  First, the socket of the on-off
// application is not created until Application Start time, so we wouldn't be
// able to hook the socket (now) at configuration time.  Second, even if we
// could arrange a call after start time, the socket is not public so we
// couldn't get at it.
//
// So, we can cook up a simple version of the on-off application that does what
// we want.  On the plus side we don't need all of the complexity of the on-off
// application.  On the minus side, we don't have a helper, so we have to get
// a little more involved in the details, but this is trivial.
//
// So first, we create a socket and do the trace connect on it; then we pass
// this socket into the constructor of our simple application which we then
// install in the source node.
// ===========================================================================
//
class MyApp : public Application
{
public:
  MyApp ();
  virtual ~MyApp ();

  /**
   * Register this type.
   * \return The TypeId.
   */
  static TypeId GetTypeId (void);
  void Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate);

private:
  virtual void StartApplication (void);
  virtual void StopApplication (void);

  void ScheduleTx (void);
  void SendPacket (void);

  Ptr<Socket>     m_socket;
  Address         m_peer;
  uint32_t        m_packetSize;
  uint32_t        m_nPackets;
  DataRate        m_dataRate;
  EventId         m_sendEvent;
  bool            m_running;
  uint32_t        m_packetsSent;
};

MyApp::MyApp ()
  : m_socket (0),
    m_peer (),
    m_packetSize (0),
    m_nPackets (0),
    m_dataRate (0),
    m_sendEvent (),
    m_running (false),
    m_packetsSent (0)
{
}

MyApp::~MyApp ()
{
  m_socket = 0;
}

/* static */
TypeId MyApp::GetTypeId (void)
{
  static TypeId tid = TypeId ("MyApp")
    .SetParent<Application> ()
    .SetGroupName ("Tutorial")
    .AddConstructor<MyApp> ()
    ;
  return tid;
}

void
MyApp::Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate)
{
  m_socket = socket;
  m_peer = address;
  m_packetSize = packetSize;
  m_nPackets = nPackets;
  m_dataRate = dataRate;
}

void
MyApp::StartApplication (void)
{
  m_running = true;
  m_packetsSent = 0;
  m_socket->Bind ();
  m_socket->Connect (m_peer);
  SendPacket ();
}

void
MyApp::StopApplication (void)
{
  m_running = false;

  if (m_sendEvent.IsRunning ())
    {
      Simulator::Cancel (m_sendEvent);
    }

  if (m_socket)
    {
      m_socket->Close ();
    }
}

void
MyApp::SendPacket (void)
{
  Ptr<Packet> packet = Create<Packet> (m_packetSize);
  m_socket->Send (packet);

  if (++m_packetsSent < m_nPackets)
    {
      ScheduleTx ();
    }
}

void
MyApp::ScheduleTx (void)
{
  if (m_running)
    {
      Time tNext (Seconds (m_packetSize * 8 / static_cast<double> (m_dataRate.GetBitRate ())));
      m_sendEvent = Simulator::Schedule (tNext, &MyApp::SendPacket, this);
    }
}

static void
CwndChange (Ptr<OutputStreamWrapper> stream, uint32_t oldCwnd, uint32_t newCwnd)
{
  NS_LOG_UNCOND (Simulator::Now ().GetSeconds () << "\t" << newCwnd);
  *stream->GetStream () << Simulator::Now ().GetSeconds () << "\t" << oldCwnd << "\t" << newCwnd << std::endl;
}

int
main (int argc, char *argv[])
{
  CommandLine cmd ;
  cmd.Parse (argc, argv);



  NodeContainer n1n3;
  n1n3.Create(2);

  NodeContainer n3n2;
  n3n2.Add (n1n3.Get (1));
  n3n2.Create (1);

  


  PointToPointHelper p1p3;
  p1p3.SetDeviceAttribute ("DataRate", DataRateValue (DataRate ("10Mbps")));
  p1p3.SetChannelAttribute ("Delay", TimeValue (MilliSeconds (3)));

  PointToPointHelper p3p2;
  p3p2.SetDeviceAttribute ("DataRate", DataRateValue (DataRate ("9Mbps")));
  p3p2.SetChannelAttribute ("Delay", TimeValue (MilliSeconds (3)));

  NetDeviceContainer devicep2p13;
  devicep2p13 = p1p3.Install (n1n3);

  NetDeviceContainer devicep2p32;
  devicep2p32 = p3p2.Install (n3n2);

  Ptr<RateErrorModel> em = CreateObject<RateErrorModel> ();
  em->SetAttribute ("ErrorRate", DoubleValue (0.00001));
  devicep2p13.Get (1)->SetAttribute ("ReceiveErrorModel", PointerValue (em));
  devicep2p32.Get (0)->SetAttribute ("ReceiveErrorModel", PointerValue (em));

  InternetStackHelper stack;
  stack.InstallAll();

  Ipv4AddressHelper ipv4;
  ipv4.SetBase ("10.1.3.0", "255.255.255.0");
  Ipv4InterfaceContainer ipInterfs13 = ipv4.Assign (devicep2p13);
  //ipv4.Assign (devicep2p13);
  ipv4.SetBase ("10.1.2.0", "255.255.255.0");
  Ipv4InterfaceContainer ipInterfs32 = ipv4.Assign (devicep2p32);

  Ipv4GlobalRoutingHelper::PopulateRoutingTables ();

  uint16_t sinkPort = 8080;
  Address sinkAddress (InetSocketAddress (ipInterfs32.GetAddress (0), sinkPort));
  Address sinkAddress2 (InetSocketAddress (ipInterfs13.GetAddress (1), sinkPort));
  PacketSinkHelper packetSinkHelper ("ns3::TcpSocketFactory", InetSocketAddress (Ipv4Address::GetAny (), sinkPort));
  ApplicationContainer sinkApps = packetSinkHelper.Install (n1n3.Get (1));
  sinkApps.Start (Seconds (0.));
  sinkApps.Stop (Seconds (30.));

  TypeId tid = TypeId::LookupByName ("ns3::TcpNewReno");
  Config::Set("/NodeList/*/$ns3::TcpL4Protocol/SocketType",TypeIdValue (tid));
  Ptr<Socket> Connection1 = Socket::CreateSocket (n1n3.Get (0), TcpSocketFactory::GetTypeId ());
  Ptr<Socket> Connection2 = Socket::CreateSocket (n1n3.Get (0), TcpSocketFactory::GetTypeId ());
  tid = TypeId::LookupByName ("ns3::TcpNewRenoCSE");
  Config::Set("/NodeList/*/$ns3::TcpL4Protocol/SocketType",TypeIdValue (tid));
  Ptr<Socket> Connection3 = Socket::CreateSocket (n3n2.Get (1), TcpSocketFactory::GetTypeId ());

  Ptr<MyApp> app = CreateObject<MyApp> ();
  app->Setup (Connection1, sinkAddress, 3000, 20000, DataRate ("1.5Mbps"));
  n1n3.Get (0)->AddApplication (app);
  app->SetStartTime (Seconds (1.));
  app->SetStopTime (Seconds (20.));

  Ptr<MyApp> app2 = CreateObject<MyApp> ();
  app2->Setup (Connection2, sinkAddress, 3000, 20000, DataRate ("1.5Mbps"));
  n1n3.Get (0)->AddApplication (app2);
  app2->SetStartTime (Seconds (5.));
  app2->SetStopTime (Seconds (25.));

  Ptr<MyApp> app3 = CreateObject<MyApp> ();
  app3->Setup (Connection3, sinkAddress2, 3000, 20000, DataRate ("1.5Mbps"));
  n3n2.Get (1)->AddApplication (app3);
  app3->SetStartTime (Seconds (15.));
  app3->SetStopTime (Seconds (30.));

  AsciiTraceHelper asciiTraceHelper;
  Ptr<OutputStreamWrapper> stream = asciiTraceHelper.CreateFileStream ("Connection1.cwnd");
  Connection1->TraceConnectWithoutContext ("CongestionWindow", MakeBoundCallback (&CwndChange, stream));

  AsciiTraceHelper asciiTraceHelper2;
  Ptr<OutputStreamWrapper> stream2 = asciiTraceHelper2.CreateFileStream ("Connection2.cwnd");
  Connection2->TraceConnectWithoutContext ("CongestionWindow", MakeBoundCallback (&CwndChange, stream2));

  AsciiTraceHelper asciiTraceHelper3;
  Ptr<OutputStreamWrapper> stream3 = asciiTraceHelper3.CreateFileStream ("Connection3.cwnd");
  Connection3->TraceConnectWithoutContext ("CongestionWindow", MakeBoundCallback (&CwndChange, stream3));

  Simulator::Stop (Seconds (30));
  Simulator::Run ();
  Simulator::Destroy ();

  return 0;
}
