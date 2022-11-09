#include "myRDMA.hpp"
#include <string.h>
#include <fstream>
#include <sstream>
#include <ctime>
#define port 40145
#define num_of_node 2
#define server_ip "192.168.0.107"

string node[num_of_node] = {server_ip,"192.168.0.108"};

char send_buffer[num_of_node][buf_size];
char recv_buffer[num_of_node][buf_size];

bool is_server(string ip){
    if(ip == server_ip)
        return true;
    return false;
}

int main(int argc, char* argv[]){
    if(argc != 2)
    {
        cerr << argv[0] << " <MY IP> " << endl;
        exit(1);
    }
    if(server_ip != node[0]){
        cerr << "node[0] is not server_ip" << endl;
        exit(1);
    }
    myRDMA myrdma;

    myrdma.initialize_rdma_connection(argv[1], node, num_of_node, port,send_buffer,recv_buffer);
 
    myrdma.create_rdma_info();
    myrdma.send_info_change_qp();

    cerr << "====================================================" << endl;
  
    string ip = argv[1];
    string msg;
    string opcode = "send"; //send, send_with_imm, write, write_with_imm

    cerr << strcmp(argv[1],server_ip) << endl;
    if (strcmp(argv[1],server_ip) == 0){
        cin >> msg;
        myrdma.rdma_send(msg,0);
    }
    else{
        myrdma.rdma_send_recv(0);
        cerr << recv_buffer[0] << endl;
    }
  

  myrdma.exit_rdma();
  
  return 0;
}
