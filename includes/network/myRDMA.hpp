#include <vector>
#include <string>
#include <iostream>
#include <stdio.h>
#include "map"
#define buf_size 1048676*30

using namespace std;
class myRDMA{
    public:
        void rdma_send_vector(vector<double> msg, int i);
        void rdma_write_vector(int i, size_t size);
        void rdma_send_pagerank(vector<double> msg, int i);
        void rdma_write_pagerank(int i);
        void rdma_recv_pagerank(int i, size_t size);
        void rdma_wrecv_pagerank(int i);
        void rdma_send(string msg, int i);
        void rdma_send_with_imm(string msg, int i);
        void rdma_write(string msg, int i);
        void rdma_write_with_imm(string msg, int i);
        void rdma_send_recv(int i);
        void rdma_send_rcv(int i, int* nn, int num_of_node, vector<double> *send, vector<double> *recv1);
        void rdma_write_recv(int i);
        void rdma_send_msg(string opcode, string msg);
        void rdma_recv_msg(string opcode, int i=0);
        void recv_t(string opcode);
        void t_recv(string opcode,int* nn, int num_of_node, vector<double> *send, vector<double> *recv1);
        void send_t(string opcode);
        void rdma_one_to_many_send_msg(string opcode, string msg);
        void rdma_one_to_many_recv_msg(string opcode);
        void rdma_many_to_one_send_msg(string opcode, string msg,vector<double> msg1);
        void rdma_many_to_one_recv_msg(string opcode);
        void rdma_comm(string opcode, string msg);
        void create_rdma_info(vector<double> *send, vector<double> *recv);
        void send_info_change_qp();
        void set_buffer(char send[][buf_size], char recv[][buf_size], int num_of_server);
        void initialize_rdma_connection(const char* ip, string server[], 
                                        int number_of_server, int Port,
                                        char send[][buf_size], char recv[][buf_size]);
        void initialize_rdma_connection_vector(const char* ip, string server[], 
                                        int number_of_server, int Port,
                                        vector<double> *send, vector<double> *recv, int num_of_vertex);
        void exit_rdma();
    private:
        std::vector<tuple<struct ibv_context*, struct ibv_pd*, 
                        int, struct ibv_cq*,
                        struct ibv_qp*, struct ibv_mr*,
                        uint16_t, uint32_t>> rdma_info[2];
      
        std::vector<pair<string,string>> qp_key;
        std::vector<pair<string,string>> qp_key1;
        char (*send_buffer)[buf_size];
        char (*recv_buffer)[buf_size];
        vector<double> *send;
        vector<double> *recv;
        vector<int> sock_idx;
        int connect_num;
        int num_of_vertex;
};