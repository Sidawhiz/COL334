import subprocess
import matplotlib.pyplot as plt 

domain_name = input();
ip_address = None
count = 0;
try :
    x = int(domain_name)
    count+=1
except:
    try:
        y = float(domain_name)
        count+=1
    except:
        pass    
finally :
    if(count!=0):
        print("Please Enter a string input : Input is either Integer or Decimal currently")
        exit()

p1 = subprocess.run(['nslookup',domain_name], capture_output=True , text = True)

if(p1.returncode!=0):
    print("Can't find ", domain_name," No answer ", "/ returncode : ", p1.returncode)
    exit()
else:
    z = p1.stdout
    k = z[-1]
    while(k=='\n'):
        z = z[:len(z)-1]
        k = z[-1]
    #print(z)
    if(k=='0' or k=='1' or k=='2' or k=='3' or k=='4' or k=='5' or k=='6' or k=='7' or k=='8' or k=='9'):
        cnt = z.count('\n')
        for i in range(cnt):
            it = z.index('\n')
            z = z[it+1:]
        it = z.index(':')
        z = z[it+2:]   
        ip_address = z
        while ip_address[0]==' ' :
            ip_address = ip_address[1:]
    else:
        print("Can't find ", domain_name," No answer ", "/ returncode : ", p1.returncode)
        exit()

print("IP address of given website is : ", ip_address)
routers = []
RTT = []
x_hops = []
concerned_message_2 = "100.0"
pr_mesage = None
hops = 0
coun = 1;
imp_letter = 's'
while (pr_mesage!=ip_address or imp_letter=='s') :
    hops+=1
    if(hops>60):
        print("Number of hops crossed 60 : exiting code")
        exit()
    try:
        p2 = subprocess.run(['ping',ip_address,'-c','1','-m',str(hops),'-t','1'], capture_output=True , text = True)
        message = p2.stdout
        imp_letter = message[-3]
        ind2 = message.index('\n')
        primary_message = message[ind2+1:]
        ind2 = primary_message.index('\n')
        primary_message = primary_message[:ind2]
        ind2 = primary_message.index('m')
        primary_message = primary_message[ind2+2:]
        ind2 = primary_message.index(':')
        primary_message = primary_message[:ind2]
        try:
            op = primary_message.index('(')
            primary_message = primary_message[primary_message.index('(')+1:primary_message.index(')')]
        except:
            pass   
        routers.append(primary_message)  
        pr_mesage = primary_message
        try:
            p3 = subprocess.run(['ping',pr_mesage,'-c','1','-t','1'], capture_output=True , text = True,timeout = 6)
            message = p3.stdout
            z = message
            try:
                cnt = z.count('\n')
                for i in range(cnt-1):
                    it = z.index('\n')
                    z = z[it+1:]
                it = z.index('=')
                z = z[it+2:] 
                it = z.index('/')
                z = z[it+1:]
                it = z.index('/')
                z = z[:it]
                primary_message = z
            except:
                primary_message = "0.000"  
            if(primary_message==None):
                primary_message = "0.000"  
        except :
            primary_message =  "0.000"    
        RTT.append(float(primary_message))
        x_hops.append(hops)
        if(coun==1):
            print ("{:<15} {:<20} {:<14}".format("hops", "IP address", "RTT"))
            coun-=1
        print ("{:<15} {:<20} {:<14}".format(hops, pr_mesage, primary_message))
        #print(message)
    except:
        routers.append("     *     ")
        RTT.append(float("0.000"))
        x_hops.append(hops)
        pr_mesage = "*"
        primary_message = "0.000"
        if(coun==1):
            print ("{:<15} {:<20} {:<14}".format("hops", "IP address", "RTT"))
            coun-=1
        print ("{:<15} {:<20} {:<14}".format(hops, pr_mesage, primary_message))


n = len(x_hops)
if(n==0):
    print("Error : Website not allowed to explore ethically")
    exit()

x = x_hops
y = RTT
plt.plot(x, y, color='green', linestyle='dashed', linewidth = 3,marker='o', markerfacecolor='blue', markersize=12)
# setting x and y axis range
plt.ylim(1,int(max(RTT))+80)
plt.xlim(1,x[-1])
  
# naming the x axis
plt.xlabel('Hop numbers')
# naming the y axis
plt.ylabel('Round Trip Time')
  
# giving a title to my graph
plt.title(domain_name)
  
# function to show the plot
plt.savefig('hop_RTT.png')