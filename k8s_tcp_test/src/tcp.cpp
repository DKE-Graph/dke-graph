#include "tcp.hpp"

int clnt_cnt=0;
int sock_cnt=0;
static int new_sock[9];
int clnt_socks[9];
std::vector<int> socks;
char name[NAME_SIZE];
char msg[BUF_SIZE];
static std::mutex mutx;

void TCP::send_msg(void * arg) //안쓰는 함수
{
   int sock=*((int*)arg);
   char name_msg[NAME_SIZE+BUF_SIZE];
   while(1) 
   {
      fgets(msg, BUF_SIZE, stdin);
      if(strcmp(msg,"exit\n")==0) 
      {
         mutx.lock();
         for(int i=0; i<clnt_cnt; i++)
            write(clnt_socks[i],  msg, strlen(msg));
         mutx.unlock();
         close(sock);
         exit(0);
      }
      sprintf(name_msg,"%s %s",name, msg);
      mutx.lock();
      for(int i=0; i<clnt_cnt; i++)
        write(clnt_socks[i],  name_msg, strlen(name_msg));
      mutx.unlock();
   }
} 
void TCP::Send_Msg(const char* m, int ip){
   char name_msg[NAME_SIZE+BUF_SIZE];
   if(strcmp(m,"exit\n")==0)
      strcpy(name_msg,m);
   else
      sprintf(name_msg,"%s %s",name, m);
   mutx.lock();
   write(clnt_socks[ip],name_msg,strlen(name_msg));
   mutx.unlock();
}
void TCP::recv_msg(void * arg) // 안쓰는 함수
{
   //int sock=*((int*)arg);
   //char name_msg[NAME_SIZE+BUF_SIZE];
   //int str_len; 
   /*while(1)
   {
      str_len=read(sock, name_msg, NAME_SIZE+BUF_SIZE-1);
      if(strcmp(name_msg,"exit\n")==0){
         close(sock);
      }
      if(str_len==-1) 
         break;
      name_msg[str_len]='\0';
      fputs(name_msg, stdout);
   }*/
}
void TCP::Recv_Msg(int ip){
   while(1){
      char name_msg[1030];
      int str_len;
      str_len = read(new_sock[ip], name_msg, 1030);
      if(strcmp(name_msg,"exit\n")==0){
         exit(1);
      }
      if(str_len==-1) 
         break;
      name_msg[str_len]='\0';
      fputs(name_msg, stdout);
   }
}
void TCP::Server(){
   serv_sock=socket(PF_INET, SOCK_STREAM, 0);

   memset(&serv_adr, 0, sizeof(serv_adr));
   serv_adr.sin_family=AF_INET;
   serv_adr.sin_addr.s_addr=htonl(INADDR_ANY);
   serv_adr.sin_port=htons(PORT1);
   
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
      clnt_adr_size=sizeof(clnt_adr);
      cout << "test" <<endl;
      clnt_sock=accept(serv_sock, (struct sockaddr*)&clnt_adr,(socklen_t*)&clnt_adr_size);
      cout << "fuck" << endl;
      if(clnt_sock == -1){
         printf("%s와 accpet error\n", inet_ntoa(clnt_adr.sin_addr));
      }
      else{
         mutx.lock();
         char s = inet_ntoa(clnt_adr.sin_addr)[12];
         clnt_socks[s-'0'] = clnt_sock;   //TCP 전송할 때 쓸 소켓을 저장
         clnt_cnt++;
         mutx.unlock();

         printf("%s와 연결 성공 \n", inet_ntoa(clnt_adr.sin_addr));
      }
   }
   //close(clnt_sock);
}
void TCP::check_ip(const char* iip){
   for(int i=0;i<NumOfServer;i++){
      if(strcmp(iip,server[i]) != 0){
         workers.push_back(std::thread(&TCP::Client,TCP(),server[i])); // 클라이언트 함수 서버 마다 쓰레드로 실행
      }
      else{
         name[0] = '[';
         name[1] = 'S';
         name[2] = 'N';
         name[3] = '0';
         name[4] = iip[12];
         name[5] = ']';
      }
   }
   sleep(2);
   for(int i=0;i<NumOfServer-1;i++){
      workers[i].detach();
   }
}
void TCP::Client(const char* iip){
   sock=socket(PF_INET, SOCK_STREAM, 0);
   memset(&serv_addr, 0, sizeof(serv_addr));
   serv_addr.sin_family=AF_INET;
   serv_addr.sin_addr.s_addr=inet_addr(iip);
   serv_addr.sin_port=htons(PORT1);
    
   while(connect(sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr))==-1){
      if(connect(sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr))==-1){
      }
   }
   new_sock[iip[12]-'0'] = sock; //TCP 수신할때 쓸 소켓 저장
   mutx.lock();
   sock_cnt++;
   mutx.unlock();
   std::thread recv = std::thread(&TCP::Recv_Msg,TCP(),iip[12]-'0');
   recv.join();
}
void TCP::Client_t(const char*iip){
   TCP::check_ip(iip);
}
void TCP::Server_t(){
   std::thread serv = std::thread(&TCP::Server,TCP()); //서버함수 쓰레드로 실행

   serv.detach();
}
map<string, string> TCP::ReadRDMAInfo(int ip){
   map<string, string> info;
    string info_name[6] = {"addr", "len", "lkey", "rkey", "lid", "qp_num"};
    for(int i = 0; i < 6; i++){
        this->result="";
        this->read_char = "";
        while(result.back() != '\n'){
            this->valread = read(new_sock[ip] , this->buffer, 1);
            this->read_char = this->buffer;
            if(this->read_char!=""){
                this->result += this->read_char;
            }
        }
        info.insert({info_name[i], this->result});
    }

    return info;
}
int *TCP::connect_sock(){
   return new_sock;
}
int *TCP::client_sock(){
   return clnt_socks;
}
int TCP::Scnt(){
   return sock_cnt;
}