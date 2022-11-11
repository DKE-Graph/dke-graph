#include "tcp.hpp"
#define port 40146
#define num_of_node 2
#define server_ip "192.168.0.107"
#define buf_size 1024 //버프사이즈 정의

string node[num_of_node] = {server_ip,"192.168.0.108"};

int main(int argc, char* argv[]){
    if(argc != 2)
    {
        std::cout << argv[0] << " MY IP" << std::endl;
        exit(1);
    }
    TCP tcp;
    tcp.connect_tcp(argv[1], node, num_of_node, port);


  return 0;
}