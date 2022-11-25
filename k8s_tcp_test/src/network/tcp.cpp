#include "tcp.hpp"

static std::mutex mutx;
TCP *tcp = new TCP();

string TCP::domain_to_ip(string domain){
   struct hostent *host;
   struct sockaddr_in addr;
   char *temp;
   
   char domain_ch[100];
   strcpy(domain_ch, domain.c_str());

   memset(&addr, '0', sizeof(addr));

   addr.sin_addr.s_addr=inet_addr(domain_ch);
   host=gethostbyaddr((char*)&addr.sin_addr, 4, AF_INET);

   int i = 0;
   while(1){
      host = gethostbyname(domain_ch);
      if(host){
         for(int i=0; host->h_addr_list[i]; i++){
            temp = inet_ntoa(*(struct in_addr*)host->h_addr_list[i]);
         }
         string result(temp);
         return result;
      }
      else{
         if(i == 0)
            printf("Progressing...\n");
         i = 2;
      }
   }
}
string TCP::check_my_ip(){
   struct ifaddrs * ifAddrStruct=NULL;
   struct ifaddrs * ifa=NULL;
   void * tmpAddrPtr=NULL;

   getifaddrs(&ifAddrStruct);

   for (ifa = ifAddrStruct; ifa != NULL; ifa = ifa->ifa_next) {
      if (!ifa->ifa_addr) {
         continue;
      }
      if (ifa->ifa_addr->sa_family == AF_INET) { // check it is IP4
         // is a valid IP4 Address
         tmpAddrPtr=&((struct sockaddr_in *)ifa->ifa_addr)->sin_addr;
         char addressBuffer[INET_ADDRSTRLEN];
         inet_ntop(AF_INET, tmpAddrPtr, addressBuffer, INET_ADDRSTRLEN);
         if(strcmp(ifa->ifa_name, "eth0") == 0){
            string str(addressBuffer);
            if (ifAddrStruct!=NULL) freeifaddrs(ifAddrStruct);
            return str;
         }
         //printf("%s IP Address %s\n", ifa->ifa_name, addressBuffer); 
      }
   }
   return "error";
}
void TCP::send_msg(const char* m, int ip){
   mutx.lock();
   write(tcp->clnt_socks[ip],m,strlen(m));
   mutx.unlock();
}
int TCP::recv_msg(int ip){
   char msg[100];
   int str_len;
   str_len=read(tcp->new_sock[ip], msg, 100);
   cout << msg << endl;
   return str_len;
}
void TCP::server(string server[]){
   serv_sock=socket(PF_INET, SOCK_STREAM, 0);
   int opt = 1;
   setsockopt(serv_sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)); 
   memset(&serv_adr, 0, sizeof(serv_adr));
   serv_adr.sin_family=AF_INET;
   serv_adr.sin_addr.s_addr=htonl(INADDR_ANY);
   serv_adr.sin_port=htons(tcp->Port);

   if(bind(serv_sock, (struct sockaddr*) &serv_adr, sizeof(serv_adr))==-1){
      std::cout << "bind() error" << std::endl;
      exit(1);
   }
   if(listen(serv_sock, 5)==-1){
      std::cout << "listen() error" << std::endl;
      exit(1);
   }
   while(1)
   {
      if(tcp->clnt_cnt == tcp->num_of_server-1){
         break;
      }
      clnt_adr_size=sizeof(clnt_adr);
      clnt_sock=accept(serv_sock, (struct sockaddr*)&clnt_adr,(socklen_t*)&clnt_adr_size);
      if(clnt_sock == -1){
         printf("%s와 accpet error\n", inet_ntoa(clnt_adr.sin_addr));
      }
      else{
         mutx.lock();
         for(int i=0;i<tcp->num_of_server;i++){
            if(inet_ntoa(clnt_adr.sin_addr) == server[i]){
               tcp->clnt_socks[i] = clnt_sock;
            }
         }
         tcp->clnt_cnt++;
         mutx.unlock();
         printf("%s: connect  [ SUCCESS ] \n", inet_ntoa(clnt_adr.sin_addr));
      }
   }
}

void TCP::client(string ip, int idx){
   sock=socket(PF_INET, SOCK_STREAM, 0);
   memset(&serv_addr, 0, sizeof(serv_addr));
   serv_addr.sin_family=AF_INET;
   serv_addr.sin_addr.s_addr=inet_addr(ip.c_str());
   serv_addr.sin_port=htons(tcp->Port);
    
   while(connect(sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr))==-1);

   tcp->new_sock[idx] = sock;

}
void TCP::client_t(const char* ip, string server[]){
   for(int i=0;i<tcp->num_of_server;i++){
      if(ip!=server[i]){
         workers.push_back(std::thread(&TCP::client,TCP(),server[i],i)); // 클라이언트 함수 서버 마다 쓰레드로 실행
      }
   }
   for(int i=0;i<tcp->num_of_server-1;i++){
      workers[i].join();
   }
}
void TCP::connect_tcp(const char* ip, string server[], int number_of_server, int Port){
   cerr << "Connecting tcp" <<endl;
   tcp->num_of_server = number_of_server;
   tcp->Port = Port;
   std::thread serv = std::thread(&TCP::server,TCP(), server); //서버함수 쓰레드로 실행
   TCP::client_t(ip, server);
   serv.join();
   cerr << "Connection completely success" << endl;
  
}
map<string, string> TCP::read_rdma_info(int ip){
   map<string, string> info;
    string info_name[6] = {"addr", "len", "lkey", "rkey", "lid", "qp_num"};
    for(int i = 0; i < 6; i++){
        this->result="";
        this->read_char = "";
        while(result.back() != '\n'){
            this->valread = read(tcp->new_sock[ip] , this->buffer, 1);
            this->read_char = this->buffer;
            if(this->read_char!=""){
                this->result += this->read_char;
            }
        }
        info.insert({info_name[i], this->result});
    }

    return info;
}


int *TCP::client_sock(){
   return tcp->clnt_socks;
}