#include "tcp.hpp"
#define buf_size 1024 //버프사이즈 정의

int main(int argc, char* argv[]){
  if(argc != 2)
  {
    std::cout << argv[0] << " MY IP" << std::endl;
    exit(1);
  }
  int socks_cnt;
  //char NAME[6];
  vector<int> server_ip;
  int idx = 0;
  char msg[buf_size];

  TCP tcp = TCP();
  cout << "Server_t() 실행" <<endl;
  tcp.Server_t();
    
  sleep(2);
    
  cout << "Client_t() 실행"<<endl;
  tcp.Client_t(argv[1]);
  cout << "자는중... 기다리세요"<<endl;
  sleep(2);
  socks_cnt = tcp.Scnt();
  cout << "자는중... 기다리세요"<<endl;
  sleep(2);
  for(int i =0;i<socks_cnt;i++){
       if (idx+1 == argv[1][12]-'0'){
            idx++;
        }
        server_ip.push_back(idx+1);
        idx++;
  }
  cout << "------채팅시작------"<<endl;
  while(1){
      fgets(msg,buf_size,stdin);

      for(int i=0;i<socks_cnt;i++){
          tcp.Send_Msg(msg,server_ip[i]);
      }
      if(strcmp(msg,"exit\n")==0)
        break;
  }

  return 0;
}