#include "tcp.hpp"
#define port 40145
#define num_of_node 2
#define server_ip "pod-a.svc-k8s-tcp-test"
#define buf_size 1024 //버프사이즈 정의

string node_domain[num_of_node] = {server_ip,"pod-b.svc-k8s-tcp-test"};
string node[num_of_node];
string my_ip;

int main(int argc, char* argv[]){
    vector<int> sock_idx;
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

    tcp.connect_tcp(my_ip.c_str(), node, num_of_node, port);


    int *clnt_socks = tcp.client_sock();
    for(int idx=0; idx < num_of_node;idx++){
        if(clnt_socks[idx]!=0){
            sock_idx.push_back(idx);
        }
    }

    char msg[100] = "hello world!";
    if (strcmp(node[0].c_str(),my_ip.c_str()) == 0){
        tcp.send_msg(msg, sock_idx[0]);
        cerr << "SEND SUCCESS" << endl;
    }
    else{
        tcp.recv_msg(sock_idx[0]);
    }

    while(1){
        cout << "이걸 보고 계신다면" << endl;
        cout << "성공 하신겁니다." << endl;
        sleep(1000);
    }

  return 0;
}