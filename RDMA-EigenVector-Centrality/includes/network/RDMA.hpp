#ifndef RDMA_H
#define RDMA_H

#include <iostream>
#include <string>
#include <fstream>
#include <sstream>
#include <cstdlib>
#include <netdb.h>
#include <cstdlib>
#include <arpa/inet.h>
#include <assert.h>
#include <inttypes.h>
#include <typeinfo>
#include <cstdio>
#include <infiniband/verbs.h>
#include <unistd.h>
#define PORT 1

using namespace std;

class RDMA{

    public:
        RDMA(){}
        ~RDMA(){}
        bool pollCompletion(struct ibv_cq* cq);
        struct ibv_context* createContext();
        struct ibv_qp* createQueuePair(struct ibv_pd* pd, struct ibv_cq* cq);
        bool changeQueuePairStateToInit(struct ibv_qp* queue_pair);
        bool changeQueuePairStateToRTR(struct ibv_qp* queue_pair, int ib_port, uint32_t destination_qp_number, uint16_t destination_local_id);
        uint16_t getLocalId(struct ibv_context* context, int ib_port);  
        uint32_t getQueuePairNumber(struct ibv_qp* qp);
        bool changeQueuePairStateToRTS(struct ibv_qp* queue_pair);
        struct ibv_mr* registerMemoryRegion(struct ibv_pd* pd, void* buffer, size_t size);
        void post_rdma_write(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key);
        void post_rdma_send_with_imm(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key);
        void post_rdma_send(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key);
        void post_rdma_write_with_imm(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key);
        void post_rdma_recv(struct ibv_qp *qp, struct ibv_mr *mr, struct ibv_cq *cq, void *addr, uint32_t length);
};      

#endif