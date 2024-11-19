#include "RDMA.hpp"

bool RDMA::pollCompletion(struct ibv_cq* cq) {
  struct ibv_wc wc;
  int result;
  int cnt = 0;
  do {
    // ibv_poll_cq returns the number of WCs that are newly completed,
    // If it is 0, it means no new work completion is received.
    // Here, the second argument specifies how many WCs the poll should check,
    // however, giving more than 1 incurs stack smashing detection with g++8 compilation.
    result = ibv_poll_cq(cq, 1, &wc);
    if(result != 0)
      break;
    cnt++;
  } while (result == 0);
  
  if (result > 0 && wc.status == ibv_wc_status::IBV_WC_SUCCESS) {
    // success
    return true;
  }

  // You can identify which WR failed with wc.wr_id.
  printf("Poll failed with status %s (work request ID: %llu)\n", ibv_wc_status_str(wc.status), wc.wr_id);
  return false;
}

struct ibv_context* RDMA::createContext() {
    
    int ret;

    ret = ibv_fork_init();
    if (ret) {
        fprintf(stderr, "Failure: ibv_fork_init (errno=%d)\n", ret);
        exit(EXIT_FAILURE);
    }

    struct ibv_device **dev_list;
    dev_list = ibv_get_device_list(NULL);

    if (!dev_list) {
        int errsave = errno;
        fprintf(stderr, "Failure: ibv_get_device_list (errno=%d)\n", errsave);
        exit(EXIT_FAILURE);        
    }

    struct ibv_context *context;

    if (dev_list[0]) {
        struct ibv_device *device = dev_list[0];
        //printf("IB device: %s\n", ibv_get_device_name(device));
        context = ibv_open_device(device);
        assert(context);
        
    }
  return context;
}

struct ibv_qp* RDMA::createQueuePair(struct ibv_pd* pd, struct ibv_cq* cq) {
  struct ibv_qp_init_attr queue_pair_init_attr;
  memset(&queue_pair_init_attr, 0, sizeof(queue_pair_init_attr));
  queue_pair_init_attr.qp_type = IBV_QPT_RC;
  queue_pair_init_attr.sq_sig_all = 1;       
  queue_pair_init_attr.send_cq = cq;         
  queue_pair_init_attr.recv_cq = cq;         
  queue_pair_init_attr.cap.max_send_wr = 1;  
  queue_pair_init_attr.cap.max_recv_wr = 1;  
  queue_pair_init_attr.cap.max_send_sge = 1; 
  queue_pair_init_attr.cap.max_recv_sge = 1; 

  return ibv_create_qp(pd, &queue_pair_init_attr);
}

bool RDMA::changeQueuePairStateToInit(struct ibv_qp* queue_pair) {
  struct ibv_qp_attr init_attr;
  memset(&init_attr, 0, sizeof(init_attr));
  init_attr.qp_state = ibv_qp_state::IBV_QPS_INIT;
  init_attr.port_num = PORT;
  init_attr.pkey_index = 0;
  init_attr.qp_access_flags = IBV_ACCESS_LOCAL_WRITE | IBV_ACCESS_REMOTE_READ | IBV_ACCESS_REMOTE_WRITE;

  return ibv_modify_qp(queue_pair, &init_attr, IBV_QP_STATE | IBV_QP_PKEY_INDEX | IBV_QP_PORT | IBV_QP_ACCESS_FLAGS) == 0 ? true : false;
}

bool RDMA::changeQueuePairStateToRTR(struct ibv_qp* queue_pair, int ib_port, uint32_t destination_qp_number, uint16_t destination_local_id) {
  struct ibv_qp_attr rtr_attr;
  memset(&rtr_attr, 0, sizeof(rtr_attr));
  rtr_attr.qp_state = ibv_qp_state::IBV_QPS_RTR;
  rtr_attr.path_mtu = ibv_mtu::IBV_MTU_1024;
  rtr_attr.rq_psn = 0;
  rtr_attr.max_dest_rd_atomic = 1;
  rtr_attr.min_rnr_timer = 0x12;
  rtr_attr.ah_attr.is_global = 0;
  rtr_attr.ah_attr.sl = 0;
  rtr_attr.ah_attr.src_path_bits = 0;
  rtr_attr.ah_attr.port_num = ib_port;
  
  rtr_attr.dest_qp_num = destination_qp_number;
  rtr_attr.ah_attr.dlid = destination_local_id;

  return ibv_modify_qp(queue_pair, &rtr_attr, IBV_QP_STATE | IBV_QP_AV | IBV_QP_PATH_MTU | IBV_QP_DEST_QPN | IBV_QP_RQ_PSN | IBV_QP_MAX_DEST_RD_ATOMIC | IBV_QP_MIN_RNR_TIMER) == 0 ? true : false;
}

uint16_t RDMA::getLocalId(struct ibv_context* context, int ib_port) {
  ibv_port_attr port_attr;
  ibv_query_port(context, ib_port, &port_attr);
  return port_attr.lid;
}

uint32_t RDMA::getQueuePairNumber(struct ibv_qp* qp) {
  return qp->qp_num;
}

bool RDMA::changeQueuePairStateToRTS(struct ibv_qp* queue_pair) {
  struct ibv_qp_attr rts_attr;
  memset(&rts_attr, 0, sizeof(rts_attr));
  rts_attr.qp_state = ibv_qp_state::IBV_QPS_RTS;
  rts_attr.timeout = 0x12;
  rts_attr.retry_cnt = 7;
  rts_attr.rnr_retry = 7;
  rts_attr.sq_psn = 0;
  rts_attr.max_rd_atomic = 1;

  return ibv_modify_qp(queue_pair, &rts_attr, IBV_QP_STATE | IBV_QP_TIMEOUT | IBV_QP_RETRY_CNT | IBV_QP_RNR_RETRY | IBV_QP_SQ_PSN | IBV_QP_MAX_QP_RD_ATOMIC) == 0 ? true : false;
}

struct ibv_mr* RDMA::registerMemoryRegion(struct ibv_pd* pd, void* buffer, size_t size) {
  return ibv_reg_mr(pd, buffer, size, IBV_ACCESS_LOCAL_WRITE | IBV_ACCESS_REMOTE_READ | IBV_ACCESS_REMOTE_WRITE);
  
}

template <typename T> 
T FromString ( const std::string &Text)
{
    std::stringstream ss(Text);
    void * result;
    ss >> result;
    return (T)result;
}

void RDMA::post_rdma_write(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key)
{
  int ret;
  r_addr.erase(r_addr.find_last_not_of(" \n\r\t")+1);

  struct ibv_sge sge = {
      .addr   = (uint64_t)(uintptr_t)addr,
      .length = length,
      .lkey   = mr->lkey,
  };

  struct ibv_send_wr send_wr = {
      .wr_id      = (uint64_t)(uintptr_t)addr,
      .next       = NULL,
      .sg_list    = &sge,
      .num_sge    = 1,
      .opcode     = IBV_WR_RDMA_WRITE,
      .send_flags = 0,
      .imm_data   = rand(),
      .wr = {
        .rdma = {
          .remote_addr = FromString<uint64_t>(r_addr),
          .rkey = stoul(r_key),
        }
      }
  };

  struct ibv_send_wr *bad_wr;
  ret = ibv_post_send(qp, &send_wr, &bad_wr);
  if(ret){
    fprintf(stderr, "Error, ibv_post_send() failed\n");
	  exit(1);
  }
  assert(ret == 0);

  //printf("post rdma write wr: imm_data=0x%08x, byte_len=%u\n", send_wr.imm_data, length);
}

void RDMA::post_rdma_write_with_imm(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key)
{
  int ret;
  r_addr.erase(r_addr.find_last_not_of(" \n\r\t")+1);

  struct ibv_sge sge = {
      .addr   = (uint64_t)(uintptr_t)addr,
      .length = length,
      .lkey   = mr->lkey,
  };

  struct ibv_send_wr send_wr = {
      .wr_id      = (uint64_t)(uintptr_t)addr,
      .next       = NULL,
      .sg_list    = &sge,
      .num_sge    = 1,
      .opcode     = IBV_WR_RDMA_WRITE_WITH_IMM,
      .send_flags = 0,
      .imm_data   = rand(),
      .wr = {
        .rdma = {
          .remote_addr = FromString<uint64_t>(r_addr),
          .rkey = stoul(r_key),
        }
      }
  };

  struct ibv_send_wr *bad_wr;
  ret = ibv_post_send(qp, &send_wr, &bad_wr);
  if(ret){
    fprintf(stderr, "Error, ibv_post_send() failed\n");
	  exit(-1);
  }
  assert(ret == 0);
}

void RDMA::post_rdma_send_with_imm(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key)
{
  int ret;
  r_addr.erase(r_addr.find_last_not_of(" \n\r\t")+1);

  struct ibv_sge sge = {
      .addr   = (uint64_t)(uintptr_t)addr, 
      .length = length,                    
      .lkey   = mr->lkey,
  };

  struct ibv_send_wr send_wr = {
      .wr_id      = (uint64_t)(uintptr_t)addr,
      .next       = NULL,
      .sg_list    = &sge,
      .num_sge    = 1,
      .opcode     = IBV_WR_SEND_WITH_IMM,
      .send_flags = 0,
      .imm_data   = htonl(0x1234)   
  };

  struct ibv_send_wr *bad_wr;
  ret = ibv_post_send(qp, &send_wr, &bad_wr);
  if(ret){
    fprintf(stderr, "Error, ibv_post_send() failed\n");
	  exit(-1);
  }
}
void RDMA::post_rdma_send(struct ibv_qp *qp, struct ibv_mr *mr, void *addr, uint32_t length, string r_addr, string r_key)
{
  int ret;
  r_addr.erase(r_addr.find_last_not_of(" \n\r\t")+1);

  struct ibv_sge sge = {
      .addr   = (uint64_t)(uintptr_t)addr, 
      .length = length,                   
      .lkey   = mr->lkey,
  };

  struct ibv_send_wr send_wr = {
      .wr_id      = (uint64_t)(uintptr_t)addr,
      .next       = NULL,
      .sg_list    = &sge,
      .num_sge    = 1,
      .opcode     = IBV_WR_SEND,
      .send_flags = 0,
      .imm_data   = htonl(0x1234)   
  };

  struct ibv_send_wr *bad_wr;
  ret = ibv_post_send(qp, &send_wr, &bad_wr);
  if(ret){
    fprintf(stderr, "Error, ibv_post_send() failed\n");
	  exit(-1);
  }
}

void RDMA::post_rdma_recv(struct ibv_qp *qp, struct ibv_mr *mr, struct ibv_cq *cq, void *addr, uint32_t length)
{
    struct ibv_sge sge = {
        .addr = (uint64_t)(uintptr_t)addr,
        .length = length,
        .lkey = mr->lkey,
    };

    struct ibv_recv_wr recv_wr = {
        .wr_id = (uint64_t)(uintptr_t)addr,
        .next = NULL,
        .sg_list = &sge,
        .num_sge = 1,
    };

    struct ibv_recv_wr *bad_wr;

    ibv_post_recv(qp, &recv_wr, &bad_wr);
}