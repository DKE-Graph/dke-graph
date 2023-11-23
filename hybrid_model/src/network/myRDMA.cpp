#include "tcp.hpp"
#include "RDMA.hpp"
#include "myRDMA.hpp"
#include <omp.h>

static std::mutex mutx;
myRDMA myrdma;
RDMA rdma;
int partition;
int partition1;

struct RdmaInfo{
    struct ibv_context* context;
    struct ibv_pd* pd;
    int cq_size;
    struct ibv_cq* cq;
    struct ibv_qp* qp;
    struct ibv_mr* mr;
    uint16_t lid;
    uint32_t qp_num;
};
std::array<std::vector<RdmaInfo>, 2> rdma_info1;
vector<double*> send_adrs;
vector<double*> recv_adrs;
char* change(string temp){
  static char stc[buf_size];
  strcpy(stc, temp.c_str());
  return stc;
}

void myRDMA::rdma_send_pagerank(vector<double> msg, int i){
    size_t size = sizeof(double)*(myrdma.num_of_vertex);
    //struct ibv_wc wc;
    
    rdma.post_rdma_send(rdma_info1[0][i].qp, rdma_info1[0][i].mr, send_adrs[i], 
                        size, myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    rdma.pollCompletion(rdma_info1[0][i].cq);
    //while(ibv_poll_cq(rdma_info1[0][i].cq,1,&wc)==0){}
 
}
void myRDMA::rdma_recv_pagerank(int i, size_t size){
    //size_t size = sizeof(double)*(myrdma.num_of_vertex);
    //struct ibv_wc wc;
    rdma.post_rdma_recv(rdma_info1[1][i].qp, rdma_info1[1][i].mr, 
                        rdma_info1[1][i].cq,recv_adrs[i], size);//sizeof(myrdma.recv[i].data()));
    rdma.pollCompletion(rdma_info1[1][i].cq);
    //while(ibv_poll_cq(rdma_info1[1][i].cq,1,&wc)==0){}
    
}
void myRDMA::rdma_write_pagerank(int i){
    //TCP tcp;
    size_t size = sizeof(double)*(myrdma.send[i].size());
    //struct ibv_wc wc;
    rdma.post_rdma_write_with_imm(rdma_info1[0][i].qp, rdma_info1[0][i].mr, send_adrs[i], 
                        size, myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    rdma.pollCompletion(rdma_info1[0][i].cq);
    //while(ibv_poll_cq(rdma_info1[0][i].cq,1,&wc)==0){}
}
void myRDMA::rdma_wrecv_pagerank(int i){
   //TCP tcp;
    struct ibv_wc wc;
    size_t size = sizeof(double)*(myrdma.num_of_vertex);
    //struct ibv_wc wc;
    rdma.post_rdma_recv(rdma_info1[1][i].qp, rdma_info1[1][i].mr, 
                        rdma_info1[1][i].cq,recv_adrs[i], size);
    while (ibv_poll_cq(rdma_info1[1][i].cq, 1, &wc) == 0);
}
void myRDMA::rdma_send_vector(vector<double> msg, int i){
    
    size_t size = sizeof(double)*(msg.size());

    rdma.post_rdma_send(rdma_info1[0][i].qp, rdma_info1[0][i].mr, send_adrs[i], 
                                size, myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    rdma.pollCompletion(rdma_info1[0][i].cq);
        //cerr << "send success" << endl;
        //cerr << "send failed" << endl;
    
}
void myRDMA::rdma_write_vector(int i, size_t size){
    //myrdma.send[i] = msg;
    //struct ibv_wc wc;
    //size_t size = sizeof(double)*(msg.size());
    rdma.post_rdma_write_with_imm(rdma_info1[0][i].qp, rdma_info1[0][i].mr, send_adrs[i], 
                                size, myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    rdma.pollCompletion(rdma_info1[0][i].cq);
    //else
    //    cerr << "send failed" << endl;
}
void myRDMA::rdma_send(string msg, int i){
    RDMA rdma;
    if (msg.size() > 67108863)
        msg.replace(67108864,67108864, "\0");
    //msg[67108865] = NULL;
    strcpy(myrdma.send_buffer[i],msg.c_str());
    
    rdma.post_rdma_send(get<4>(myrdma.rdma_info[0][i]), get<5>(myrdma.rdma_info[0][i]), myrdma.send_buffer[i], 
                                sizeof(myrdma.send_buffer[i]), myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    if(!rdma.pollCompletion(get<3>(myrdma.rdma_info[0][i])))
        //cerr << "send success" << endl;
        cerr << "send failed" << endl;
    
}

void myRDMA::rdma_send_with_imm(string msg, int i){
    RDMA rdma;
    
    if (msg.size() > 67108863)
        msg.replace(67108864,67108864, "\0");
    //msg[67108865] = NULL;
    strcpy(myrdma.send_buffer[i],msg.c_str());
    
    rdma.post_rdma_send_with_imm(get<4>(myrdma.rdma_info[0][i]), get<5>(myrdma.rdma_info[0][i]), myrdma.send_buffer[i], 
                                sizeof(myrdma.send_buffer[i]), myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    if(!rdma.pollCompletion(get<3>(myrdma.rdma_info[0][i])))
        //cerr << "send success" << endl;
        cerr << "send failed" << endl;
}

void myRDMA::rdma_write(string msg, int i){
    RDMA rdma;
    TCP tcp;
    //if (msg.size() > 67108863)
    //    msg.replace(67108864,67108864, "\0");
    //msg[67108865] = NULL;
    //strcpy(myrdma.send_buffer[i],msg.c_str());
    
    rdma.post_rdma_write(get<4>(myrdma.rdma_info[0][i]), get<5>(myrdma.rdma_info[0][i]), myrdma.send_buffer[i], 
                         sizeof(myrdma.send_buffer[i]), myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    if(rdma.pollCompletion(get<3>(myrdma.rdma_info[0][i]))){
        //cerr << "send success" << endl;
        tcp.send_msg("1", myrdma.sock_idx[i]);
    }
    else
        cerr << "send failed" << endl;
}
void myRDMA::rdma_write_with_imm(string msg, int i){
    RDMA rdma;
    
    if (msg.size() > 67108863)
        msg.replace(67108864,67108864, "\0");
    //msg[67108865] = NULL;
    strcpy(myrdma.send_buffer[i],msg.c_str());
    
    rdma.post_rdma_write_with_imm(get<4>(myrdma.rdma_info[0][i]), get<5>(myrdma.rdma_info[0][i]), myrdma.send_buffer[i], 
                                sizeof(myrdma.send_buffer[i]), myrdma.qp_key[i].first, myrdma.qp_key[i].second);
    if(!rdma.pollCompletion(get<3>(myrdma.rdma_info[0][i])))
        //cerr << "send success" << endl;
        cerr << "send failed" << endl;
    
}
void myRDMA::rdma_send_recv(int i){
    //RDMA rdma;
    //vector<long double> x1;
    size_t size = sizeof(double)*(myrdma.num_of_vertex);
   
    rdma.post_rdma_recv(rdma_info1[1][i].qp, rdma_info1[1][i].mr, 
                        rdma_info1[1][i].cq, recv_adrs[i], size);//sizeof(myrdma.recv[i].data()));
    rdma.pollCompletion(rdma_info1[1][i].cq);
    //if(!rdma.pollCompletion(get<3>(myrdma.rdma_info[1][i])))
    //    cerr << "recv failed" << endl;
    //else{
        //cerr << strlen(myrdma.recv_buffer[i])/(1024*1024) <<"Mb data ";
        
        
        //for(int j=0;j<20;j++){
        //    cout << j << ": " << myrdma.recv[i][j] << endl;
        //}
        //x = &myrdma.recv[i];
        //cout.precision(numeric_limits<double>::digits10);
        //cerr << "receive success" << endl;
        
    
    //}
}
void myRDMA::rdma_send_rcv(int i, int* nn, int num_of_node, vector<double> *send, vector<double> *recv1){
    RDMA rdma;
    //vector<long double> x1;
    size_t size = sizeof(double)*(myrdma.num_of_vertex);
   
    rdma.post_rdma_recv(rdma_info1[1][i].qp, rdma_info1[1][i].mr, 
                        rdma_info1[1][i].cq, recv_adrs[i], size);//sizeof(myrdma.recv[i].data()));
    rdma.pollCompletion(rdma_info1[1][i].cq);

    //size = nn[i];
    //send[0].insert(send[0].end(),make_move_iterator(recv1[i].begin()),make_move_iterator(recv1[i].begin() + size));
    //if(!rdma.pollCompletion(get<3>(myrdma.rdma_info[1][i])))
    //    cerr << "recv failed" << endl;
    //else{
        //cerr << strlen(myrdma.recv_buffer[i])/(1024*1024) <<"Mb data ";
        
        
        //for(int j=0;j<20;j++){
        //    cout << j << ": " << myrdma.recv[i][j] << endl;
        //}
        //x = &myrdma.recv[i];
        //cout.precision(numeric_limits<double>::digits10);
        //cerr << "receive success" << endl;
        
    
    //}
}
void myRDMA::rdma_write_recv(int i){
    TCP tcp;
    while(tcp.recv_msg(myrdma.sock_idx[i]) <= 0);
    //cerr << strlen(myrdma.recv_buffer[i])/(1024*1024) <<"Mb data ";
    //cerr << "recv success" << endl;
}

void myRDMA::rdma_send_msg(string opcode, string msg){
    if (opcode == "send_with_imm"){
        cerr << "rdma_send_with_imm run" <<endl;
        for(int i=0;i<myrdma.connect_num;i++){
            myRDMA::rdma_send_with_imm(msg, i);
        }
    }
    else if(opcode == "write"){
        cerr << "rdma_write run" << endl;
        for(int i=0;i<myrdma.connect_num;i++){
            myRDMA::rdma_write(msg, i);
        }
    }
    else if(opcode == "write_with_imm"){
        //vector<double> a;
        //cerr << "write_with_imm_rdma run" <<endl;
        for(int i=0;i<myrdma.connect_num;i++){
            myRDMA::rdma_write_vector(i, sizeof(double) * 1);
        }
    }
    else if(opcode == "send"){
        cerr << "rdma_send run" <<endl;
        for(int i=0;i<myrdma.connect_num;i++){
            myRDMA::rdma_send(msg, i);
        }
    }
    else{
        cerr << "rdma_send_msg opcode error" << endl;
        exit(1);
    }
}
void myRDMA::rdma_recv_msg(string opcode, int i){
    if (opcode == "send_with_imm" || opcode == "write_with_imm" || opcode == "send"){
        myRDMA::rdma_send_recv(i);
    }
    else if(opcode == "write"){
        myRDMA::rdma_write_recv(i);
    }
    else{
        cerr << "rdma_recv_msg opcode error" << endl;
        exit(1);
    }
}
void myRDMA::recv_t(string opcode){
    std::vector<std::thread> worker;
    //worker.reserve(myrdma.connect_num);
    //omp_set_num_threads(myrdma.connect_num);
    if (opcode == "send_with_imm" || opcode == "write_with_imm" || opcode == "send"){
        //#pragma omp parallel
        for(int i=0;i<myrdma.connect_num;i++){
            //myRDMA::rdma_send_recv(i);
            worker.push_back(std::thread(&myRDMA::rdma_send_recv,myRDMA(),i));
        }
    }
    else if(opcode == "write"){
        for(int i=0;i<myrdma.connect_num;i++){
            worker.push_back(std::thread(&myRDMA::rdma_write_recv,myRDMA(),i));
        }
    }
    else{
        cerr << "recv_t opcode error" << endl;
        exit(1);
    }
    for(int i=0;i<myrdma.connect_num;i++){
        worker[i].join();
    }
}

void myRDMA::t_recv(string opcode,int* nn, int num_of_node, vector<double> *send, vector<double> *recv1){
    std::vector<std::thread> worker;
    size_t size;
    worker.reserve(myrdma.connect_num);
    
    if (opcode == "send_with_imm" || opcode == "write_with_imm" || opcode == "send"){
        for(int i=0;i<myrdma.connect_num;i++){
            worker.push_back(std::thread(&myRDMA::rdma_send_rcv,myRDMA(),i, nn, num_of_node, send, recv1));
        }
    }
    for(int i=0;i<myrdma.connect_num;i++){
        size = nn[i];
        send[0].insert(send[0].end(),make_move_iterator(recv1[i].begin()),make_move_iterator(recv1[i].begin() + size));
        worker[i].join();
    }
}
void myRDMA::send_t(string opcode){
    std::vector<std::thread> worker;
    worker.reserve(myrdma.connect_num);
    for(int i=0;i<myrdma.connect_num;i++)
         worker.push_back(std::thread(&myRDMA::rdma_send_pagerank,myRDMA(),myrdma.send[0],i));
    for(int i=0;i<myrdma.connect_num;i++)
        worker[i].join();
      
}

void myRDMA::rdma_comm(string opcode, string msg){;
    thread snd_msg = thread(&myRDMA::rdma_send_msg,myRDMA(),opcode,msg);
    myRDMA::recv_t(opcode);

    snd_msg.join();
}

void myRDMA::rdma_one_to_many_send_msg(string opcode, string msg){
    myRDMA::rdma_send_msg(opcode, msg);
}

void myRDMA::rdma_one_to_many_recv_msg(string opcode){
    myRDMA::rdma_recv_msg(opcode);
}

void myRDMA::rdma_many_to_one_send_msg(string opcode, string msg, vector<double> msg1){
    if (opcode == "send_with_imm"){
        cerr << "rdma_send_with_imm run" <<endl;
        myRDMA::rdma_send_with_imm(msg, 0);
    }
    else if(opcode == "write"){
        cerr << "rdma_write run" << endl;
        //myRDMA::rdma_write_vector(msg1, 0);

    }
    else if(opcode == "write_with_imm"){
        cerr << "write_with_imm_rdma run" <<endl;
        myRDMA::rdma_write_with_imm(msg, 0);
    }
    else if(opcode == "send"){
        myRDMA::rdma_send_vector(msg1, 0);
    }
    else{
        cerr << "rdma_many_to_one_send_msg opcode error" << endl;
        exit(1);
    }
}
void myRDMA::rdma_many_to_one_recv_msg(string opcode){
    myRDMA::recv_t(opcode);
    /*for(int i=0;i<252*4;i++){
        cout << myrdma.send[0][i] << endl;
    }*/
}

void myRDMA::send_info_change_qp(){
    TCP tcp;
    RDMA rdma;
    //Send RDMA info
    for(int k = 0;k<2;k++){
        int *clnt_socks = tcp.client_sock();
        cout << "[INFO]SEND RDMA INFO[" << k << "] ";
        if(k==0){
            for(int idx=0; idx < myrdma.connect_num+1; idx++){
                if(clnt_socks[idx]!=0){
                    myrdma.sock_idx.push_back(idx);
                }
            }
        }
        for(int j=0;j<myrdma.connect_num;j++){
            std::ostringstream oss;

            if(k==0){
                oss << myrdma.send[j].data();
                if(tcp.check_my_ip() == "192.168.0.100"){
                    oss << myrdma.send[0].data();
                }
            }
            else if(k==1)
                oss << myrdma.recv[j].data();
            
            tcp.send_msg(change(oss.str()+"\n"),myrdma.sock_idx[j]);
            tcp.send_msg(change(to_string(rdma_info1[k][j].mr->length)+"\n"),myrdma.sock_idx[j]);
            tcp.send_msg(change(to_string(rdma_info1[k][j].mr->lkey)+"\n"),myrdma.sock_idx[j]);
            tcp.send_msg(change(to_string(rdma_info1[k][j].mr->rkey)+"\n"),myrdma.sock_idx[j]);
            tcp.send_msg(change(to_string(rdma_info1[k][j].lid)+"\n"),myrdma.sock_idx[j]);
            tcp.send_msg(change(to_string(rdma_info1[k][j].qp_num)+"\n"),myrdma.sock_idx[j]);
            
        }
        cout<< "- SUCCESS" <<endl;
        //Read RDMA info
        map<string, string> read_rdma_info;
        cout << "[INFO]CHANGE QUEUE PAIR STATE ";
        for(int i=0;i<myrdma.connect_num;i++){
            if(k == 0 || k == 1){
                read_rdma_info = tcp.read_rdma_info(myrdma.sock_idx[i]);
                //Exchange queue pair state
                rdma.changeQueuePairStateToInit(rdma_info1[k^1][i].qp);
                rdma.changeQueuePairStateToRTR(rdma_info1[k^1][i].qp, PORT, 
                                               stoi(read_rdma_info.find("qp_num")->second), 
                                               stoi(read_rdma_info.find("lid")->second));
                
                if(k^1==0){
                    rdma.changeQueuePairStateToRTS(rdma_info1[k^1][i].qp);
                    myrdma.qp_key.push_back(make_pair(read_rdma_info.find("addr")->second,
                                                      read_rdma_info.find("rkey")->second));
                }   
            }
        }
        cout << "- SUCCESS" << endl;
    }
    //cerr << "Completely success" << endl;
}
void myRDMA::create_rdma_info(vector<double> *send, vector<double> *recv){
    RDMA rdma;
    TCP tcp;
    cout << "[INFO]CREATE RDMA INFO ";
   
    for(int j =0;j<2;j++){
        
        if(j == 1){
            for(int i =0;i<myrdma.connect_num;i++){
                struct ibv_context* context = rdma.createContext();
                struct ibv_pd* protection_domain = ibv_alloc_pd(context);
                int cq_size = 0x10;
                struct ibv_cq* completion_queue = ibv_create_cq(context, cq_size, nullptr, nullptr, 0);
                struct ibv_qp* qp = rdma.createQueuePair(protection_domain, completion_queue);
                struct ibv_mr *mr = rdma.registerMemoryRegion(protection_domain, 
                                                        recv[i].data(), sizeof(double)*(recv[i].size()));//recv[i].size()));//sizeof(myrdma.recv[i].data()));
                uint16_t lid = rdma.getLocalId(context, PORT);
                uint32_t qp_num = rdma.getQueuePairNumber(qp);
                rdma_info1[j].emplace_back(RdmaInfo{context,protection_domain,cq_size,completion_queue,qp,mr,lid,qp_num});
            }
        }
        else{
            for(int i =0;i<myrdma.connect_num;i++){
                struct ibv_context* context = rdma.createContext();
                //struct ibv_pd* protection_domain = ibv_alloc_pd(context);
                //cout << "ibv_pd" << endl;
                int cq_size = 0x10;
                struct ibv_cq* completion_queue = ibv_create_cq(context, cq_size, nullptr, nullptr, 0);
                struct ibv_pd* protection_domain = ibv_alloc_pd(context);
                struct ibv_qp* qp = rdma.createQueuePair(protection_domain, completion_queue);
                struct ibv_mr *mr = rdma.registerMemoryRegion(protection_domain, 
                                                        send[i].data(), sizeof(double)*(send[i].size()));//send[i].size()));//sizeof(myrdma.send[i].data()));
                uint16_t lid = rdma.getLocalId(context, PORT);
                uint32_t qp_num = rdma.getQueuePairNumber(qp);
                rdma_info1[j].emplace_back(RdmaInfo{context,protection_domain,cq_size,completion_queue,qp,mr,lid,qp_num});
            }
        }
        //}
      
    }
    
    cout << " - SUCCESS" << endl;
}
void myRDMA::set_buffer(char send[][buf_size], char recv[][buf_size], int num_of_server){
    myrdma.send_buffer = &send[0];
    myrdma.recv_buffer = &recv[0];
    myrdma.connect_num = num_of_server - 1;
}

void myRDMA::initialize_rdma_connection(const char* ip, string server[], int number_of_server, int Port, char send[][buf_size], char recv[][buf_size]){
    TCP tcp;
    tcp.connect_tcp(ip, server, number_of_server, Port);
    //myrdma.send_buffer = &send[0];
    //myrdma.recv_buffer = &recv[0];
    myrdma.connect_num = number_of_server - 1;
}
void myRDMA::initialize_rdma_connection_vector(const char* ip, string server[], int number_of_server, int Port, vector<double> *send, vector<double> *recv, int num_of_vertex){
    TCP tcp;
    tcp.connect_tcp(ip, server, number_of_server, Port);
    myrdma.send = &send[0];
    myrdma.recv = &recv[0];
    myrdma.num_of_vertex = num_of_vertex;

    //myRDMA::initialize_memory_pool();
    int n = num_of_vertex/(number_of_server-1);
    partition=n;
    int n1 = num_of_vertex - n*(number_of_server-2);
    partition1=n1;
   
    //cout << partition << " " << partition1 << endl;
    for(int i=0;i<number_of_server-1;i++){
        //myrdma.send[i].resize(num_of_vertex);
        //myrdma.recv[i].resize(num_of_vertex);
        send_adrs.push_back(myrdma.send[i].data());
        recv_adrs.push_back(myrdma.recv[i].data());
    }
    rdma_info1[0].reserve(100000);
    rdma_info1[1].reserve(100000);
    
    myrdma.connect_num = number_of_server - 1;
}
void myRDMA::exit_rdma(){
    for(int j=0;j<2;j++){
        for(int i=0;i<myrdma.connect_num;i++){
            ibv_destroy_qp(rdma_info1[j][i].qp);
            ibv_dereg_mr(rdma_info1[j][i].mr);
            ibv_destroy_cq(rdma_info1[j][i].cq);
            ibv_dealloc_pd(rdma_info1[j][i].pd);
            ibv_close_device(rdma_info1[j][i].context);
        }
    }
}


