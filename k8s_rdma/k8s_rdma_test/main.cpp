#include "myRDMA.hpp"
#include "tcp.hpp"

#define port 40145
#define num_of_node 2
#define server_ip "pod-a.svc-k8s-rdma-test"

string node_domain[num_of_node] = {server_ip,"pod-b.svc-k8s-rdma-test","pod-c.svc-k8s-rdma-test","pod-d.svc-k8s-rdma-test"};
string node[num_of_node];
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

    TCP tcp;

    cout << "check my ip" << endl;
    my_ip = tcp.check_my_ip();
    cout << "finish! this pod's ip is " <<my_ip << endl;

    cout << "Changing domain to ip ..." << endl;
    for(int i = 0 ;i < num_of_node;i++){
        node[i]=tcp.domain_to_ip(node_domain[i]);
        cout << node_domain[i] << " ----> " << node[i] <<endl;
    }
    cout << "Success" << endl;

    myRDMA myrdma;
    
    myrdma.initialize_rdma_connection(my_ip.c_str(), node, num_of_node, port,send_buffer,recv_buffer);
 
    myrdma.create_rdma_info();
    myrdma.send_info_change_qp();

    cerr << "====================================================" << endl;
  
    string msg;
    string opcode = "send"; //send, send_with_imm, write, write_with_imm
    
    msg = "[ " + my_ip + " ] Hello k8s RDMA";
    myrdma.rdma_comm(opcode, msg);
    printf("recv_buffer[0]: %s\n", recv_buffer[0]); 
    

    myrdma.exit_rdma();
  
    sleep(5);
    while(1){
        cerr << "Success RDMA communication" <<endl;
        sleep(1000);
    }
  
    return 0;
}
