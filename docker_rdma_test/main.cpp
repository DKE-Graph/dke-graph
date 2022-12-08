#include "myRDMA.hpp"
#include "tcp.hpp"
#include <infiniband/verbs.h>

#define port 40145
#define num_of_node 2
#define server_ip "172.17.0.2"

string node_domain[num_of_node] = {server_ip,"172.17.0.3"};
string node[num_of_node] = {server_ip, "172.17.0.3"};
string my_ip;

char send_buffer[num_of_node][buf_size];
char recv_buffer[num_of_node][buf_size];

bool is_server(string ip){
    if(ip == server_ip)
        return true;
    return false;
}

int main(int argc, char* argv[]){
    if(server_ip != node_domain[0]){
        cerr << "node[0] is not server_ip" << endl;
        exit(1);
    }
    struct ibv_device **dev_list;
    dev_list = ibv_get_device_list(NULL);
    cout << dev_list << endl;

    TCP tcp;

    cout << "check my ip" << endl;
    my_ip = tcp.check_my_ip();
    cout << "finish! this pod's ip is " <<my_ip << endl;

    myRDMA myrdma;
    
    myrdma.initialize_rdma_connection(my_ip.c_str(), node, num_of_node, port,send_buffer,recv_buffer);
 
    myrdma.create_rdma_info();
    myrdma.send_info_change_qp();

    cerr << "====================================================" << endl;
  
    string msg;
    string opcode = "send"; //send, send_with_imm, write, write_with_imm

    if (strcmp(my_ip.c_str(),node[0].c_str()) == 0){
        msg = "Hello Docker RDMA";
        myrdma.rdma_send(msg,0);
    }
    else{
        myrdma.rdma_send_recv(0);
        cerr << recv_buffer[0] << endl;
    }
  

  myrdma.exit_rdma();
  
  return 0;
}
