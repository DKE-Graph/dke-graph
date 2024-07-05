#include <mpi.h>
#include <stdio.h>
#include<stdlib.h>
#include<time.h>
#include<string.h>
#include <omp.h>
#include <iostream>
#include <vector>
#include <string>
#include <fstream>
#include <math.h>
#include <algorithm>
#include <unistd.h>
#include <myRDMA.hpp>
#include <pagerank.hpp>
#include <numeric>
#include "tcp.hpp"
#include <mutex>
#include <queue>
#include <condition_variable>
#include <future>
#define df 0.85
#define MAX 100000
#define MAXX 50000
#define num_of_node 5
#define port 40145
#define server_ip "192.168.4.5"//"pod-a.svc-k8s-rdma"
//7,9,6,5,2,3,4,8
string node[num_of_node] = {server_ip,"192.168.4.9","192.168.4.8","192.168.4.10","192.168.4.11"};//"192.168.4.14","192.168.4.13","192.168.4.12","192.168.4.16"};//,"192.168.1.106","192.168.1.107","192.168.1.108","192.168.1.109"};//"pod-b.svc-k8s-rdma","pod-c.svc-k8s-rdma","pod-d.svc-k8s-rdma","pod-e.svc-k8s-rdma"};//,"192.168.1.102","192.168.1.103"};

string node_domain[num_of_node];

std::vector<int> num_outgoing;
int num_of_vertex;
int start, end;
int edge;
int max_edge = 0;
using namespace std;
void normalize(std::vector<double>& vec, std::vector<double>& vec1) {
    double norm = std::sqrt(std::inner_product(vec1.begin(), vec1.end(), vec1.begin(), 0.0));
    for (auto& val : vec) {
        val /= norm;
    }
}

bool is_server(string ip){
  if(ip == server_ip)
    return true;
  return false;
}

int main(int argc, char** argv){
    TCP tcp;
    Pagerank pagerank;
    myRDMA myrdma;
    int rank, size, i ,j;
    int start, end;
    int a,b;
    //int argvv = stoi(argv[3]);
    long double network_time = 0;
    long double compute_time = 0;
    long double avg_compute_time = 0;
    struct timespec begin1, end1 ;
    struct timespec begin2, end2 ;
    struct timespec begin3, end3 ;
    std::vector<std::vector<size_t>>* graph = new std::vector<std::vector<size_t>>();
    std::vector<std::vector<size_t>> sliced_graph; //= new std::vector<std::vector<size_t>>();
    std::vector<std::vector<size_t>> slice_graph;
    vector<double> send[num_of_node];
    vector<double> recv1[num_of_node];
    vector<double>* send_first = &send[1];
    vector<double>* send_end = &send[num_of_node-1];
    int my_idx;
    int num_vertex;
    int num_edge = 0;
    size_t buff_size;
    size_t div_buff_size;

    string my_ip= tcp.check_my_ip();
    
    //MPI Init=====================================================================
    
    MPI_Init(&argc, &argv);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    // Create Graph================================================================
    int recvcounts[size];
    int displs[size]; 
    int nn[num_of_node];

    if(rank == 0){
        
        cout << "[INFO]IP: " << my_ip << endl;
        if(my_ip != server_ip){
            cout << "=====================================================" << endl;
            cout << "[INFO]CREATE GRAPH" << endl;
        }
        else{
            cout << "=====================================================" << endl;
            cout << "[INFO]NETWORK CONFIGURATION" << endl;
        }
    }
    
    

    //Graph Partitioning=============================================================
    pagerank.create_vertex_weight(argv[1],argv[2], num_outgoing, num_of_vertex, 
                                start, end, nn, num_of_node, size, node, my_ip, 
                                rank, displs, recvcounts, send, recv1,argv[3]);
    num_of_vertex = num_outgoing.size();

    if(rank == 0 && my_ip != server_ip)
        cout << "[INFO]TOTAL VERTEX: "<<num_of_vertex << endl;

    pagerank.create_sliced_graph(argv[1],argv[2],start, end, sliced_graph, rank);
    

    
    
    size_t innerVectorsSize = 0;
    for (const auto& innerVector : sliced_graph) {
        innerVectorsSize += innerVector.size() * sizeof(size_t);
    }
    size_t totalSize = innerVectorsSize;
    
    size_t outgoing_size = sizeof(size_t) * num_outgoing.size();
    
    if(rank == 0 && my_ip != server_ip){
        cout << "[INFO]FINISH CREATE GRAPH " <<endl;
        num_vertex = end-start;
        num_edge =0;

        for(int i=start; i<end;i++){
            num_edge += num_outgoing[i];
        }
        cout << "[INFO]Vertex: " << num_vertex << ", Edge: " << num_edge << endl;
        
        cout << "[INFO]GRAPH MEMORY USAGE: " << totalSize << " + " <<outgoing_size << "= " << totalSize+outgoing_size << " byte." << endl;
    }
    
  

    //Delete Graph===================================================================
    if(my_ip == server_ip){
        num_outgoing.clear();
        num_outgoing.shrink_to_fit();
    }

    //D-RDMALib Init===================================================================
    
    if(rank == 0){
        if(my_ip != server_ip){
            cout << "=====================================================" << endl;
            cout << "[INFO]NETWORK CONFIGURATION" << endl;
        }
        myrdma.initialize_rdma_connection_vector(my_ip.c_str(),node,num_of_node,port,send,recv1,num_of_vertex);
        myrdma.create_rdma_info(send, recv1);
        myrdma.send_info_change_qp();
    }
    div_buff_size = sizeof(double) * send[0].size();
   
 
    int check;
    int check1[size];
    
    size_t step;
    double diff=1;
    double dangling_pr = 0.0;
    vector<double> prev_pr;
    double df_inv = 1.0 - df;
    double inv_num_of_vertex = 1.0 / num_of_vertex;
    
    
    long double time3;
    long double sum_time3 = 0;
    long double mpi_time = 0;
    long double rdma_time = 0;
    

    vector<double> div_send;
    double* send_buf_ptr;
    
    if(my_ip != node[0] && size > 1){
        div_send.resize(end-start);
        send_buf_ptr = div_send.data();
        
    }
    else if(my_ip != node[0] && size <= 1){
        send_buf_ptr = send[0].data();
    }

    if(my_ip == node[0]){
        send_buf_ptr = send[0].data();
    }
    
    double* recv_buffer_ptr = recv1[0].data();
   
    

    check = 1;
    MPI_Allgather(&check, 1, MPI_INT, check1, 1, MPI_INT, MPI_COMM_WORLD);
    if(rank == 0){
        myrdma.rdma_comm("write_with_imm", "1");
    }
    MPI_Allgather(&check, 1, MPI_INT, check1, 1, MPI_INT, MPI_COMM_WORLD);
    

    if(rank == 0 || my_ip == node[0])
        cout <<"=====================================================" <<endl;      

    clock_gettime(CLOCK_MONOTONIC, &begin2);
    std::fill(send[0].begin(), send[0].end(), 1.0 / std::sqrt(num_of_vertex));
    std::fill(recv1[0].begin(), recv1[0].end(), 1.0 /std::sqrt(num_of_vertex));
    //EigenVector Calculation===============================================================================
    for(step =0;step<25;step++){
        //cout << recv1[0][0] << endl;
        if(rank == 0 || my_ip == node[0]){
            cout <<"================STEP "<< step+1 << "================" <<endl;
            cout << "PR[2049138]: " <<recv1[0][2049138] << endl;
        }
        dangling_pr = 0.0;
        if(step!=0) {
            clock_gettime(CLOCK_MONOTONIC, &begin1);
            if(my_ip == node[0]){
                diff = 0;
                for (size_t i=0;i<num_of_vertex;i++) 
                    diff += fabs(prev_pr[i] - send[0][i]);
            }
            clock_gettime(CLOCK_MONOTONIC, &end1);
            time3 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
            compute_time+=time3;
        }
        //===============================================================================
        if(my_ip != node[0]){
            //const size_t sg_size = sliced_graph.size();
            if(rank == 0)
                cout << "[INFO]COMPUTE EIGENVECTOR ";
            clock_gettime(CLOCK_MONOTONIC, &begin1);
            int idx;
            
            std::fill(send[0].begin(), send[0].end(), 0.0);
            for(size_t i = 0; i < end - start; ++i) {
                for (size_t neighbor : sliced_graph[i]) {
                    //if(start + i == 2049138)
                        
                        //cout << send[0][i] <<", " << recv1[0][neighbor] << endl;
                    //cout << send_buf_ptr[i] << ", " << recv_buffer_ptr[neighbor] << endl;
                    send[0][i] += recv1[0][neighbor];
                }
            }
            
            normalize(send[0], recv1[0]);
            /*for(size_t i=start-start;i<end-start;i++){
                //cout << i << endl;
                //
                idx = i;
                double tmp = 0.0;
                const size_t graph_size = sliced_graph[i].size();
                const size_t* graph_ptr = sliced_graph[i].data();
                for(size_t j=0; j<graph_size; j++){
                    const size_t from_page = graph_ptr[j];
                    const double inv_num_outgoing = 1.0 / num_outgoing[from_page];
                    tmp += recv_buffer_ptr[from_page] * inv_num_outgoing;
                }
                send_buf_ptr[idx] = (tmp + dangling_pr * inv_num_of_vertex) * df + df_inv * inv_num_of_vertex;
            }*/
            clock_gettime(CLOCK_MONOTONIC, &end1);
            time3 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
            compute_time += time3;
            //cout << rank << ", " << time3 << endl;
            if(rank == 0)
                cout << "- SUCCESS" << endl;
            //printf("%d: calc 수행시간: %Lfs.\n", rank, time3);
            //MPI_Allgather(&check, 1, MPI_INT, check1, 1, MPI_INT, MPI_COMM_WORLD);
            //---------------------------------------------------------------------------------------------------------------------
            clock_gettime(CLOCK_MONOTONIC, &begin1);
            
            if(size > 1)
                MPI_Allgatherv(send_buf_ptr,div_send.size(),MPI_DOUBLE,send[0].data(),recvcounts,displs,MPI_DOUBLE,MPI_COMM_WORLD);
            
            clock_gettime(CLOCK_MONOTONIC, &end1);
            time3 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
            
            if(rank ==0){
                //cout << "[INFO]START MPI_ALLGATHERV - SUCCESS ";
                //cout << time3 << "s." <<endl;
                //printf("%Lfs\n", time3);
                network_time += time3;
                mpi_time += time3;
            }    
            //printf("%d: allgatherv 수행시간: %Lfs.\n", rank, time3);
            //long double time1 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
            
            //MPI_Allgather(div_send.data(),div_send.size(),MPI_DOUBLE,send[0].data(),div_send.size(),MPI_DOUBLE,MPI_COMM_WORLD);
        }
        else{
            prev_pr = send[0];
        }
        //===============================================================================
        clock_gettime(CLOCK_MONOTONIC, &begin1);
        if(my_ip == node[0]){
            send[0].clear();
            //clock_gettime(CLOCK_MONOTONIC, &begin3);
            myrdma.recv_t("send");
            //clock_gettime(CLOCK_MONOTONIC, &end3);
            long double time3 = (end3.tv_sec - begin3.tv_sec) + (end3.tv_nsec - begin3.tv_nsec) / 1000000000.0;
            //cout << time3 << endl;
            //myrdma.t_recv("send", nn, num_of_node, send, recv1);
            cout << "[INFO]START RECEIVE - SUCCESS" << endl;
            
            //clock_gettime(CLOCK_MONOTONIC, &begin3);
            
            for(size_t i=0;i<num_of_node-1;i++){
                size = nn[i];
                //std::vector<double>::iterator iterator = recv1[i].begin();
                send[0].insert(send[0].end(),make_move_iterator(recv1[i].begin()),make_move_iterator(recv1[i].begin() + size));
            }   
            //clock_gettime(CLOCK_MONOTONIC, &end3);
            //time3 = (end3.tv_sec - begin3.tv_sec) + (end3.tv_nsec - begin3.tv_nsec) / 1000000000.0;
            //cout << time3 << endl;

            //if(diff < 1e-6)
            //    send_buf_ptr[0] += 1; 
            
            
            //myrdma.rdma_write_pagerank(0);
            //clock_gettime(CLOCK_MONOTONIC, &begin3);
            
            fill(send_first, send_end, send[0]);
            //clock_gettime(CLOCK_MONOTONIC, &end3);
            //time3 = (end3.tv_sec - begin3.tv_sec) + (end3.tv_nsec - begin3.tv_nsec) / 1000000000.0;
            //cout << time3 << endl;
            cout << "[INFO]START AGGREGATE - SUCCESS" << endl;
        }
        else{
            if(rank == 0){
                cout << "[INFO]START SEND_RDMA - SUCCESS "<< endl;
                myrdma.rdma_write_vector(0,div_buff_size);
                //myrdma.rdma_recv_pagerank(0);
            }
            
            
        }
        clock_gettime(CLOCK_MONOTONIC, &end1);
        long double time1 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
        if(rank == 0){
            network_time+=time1;
            rdma_time+=time1;
        }
        //printf("%d: send 수행시간: %Lfs.\n", rank, time1); 
        //===============================================================================
        if(my_ip == node[0]){
            //cout << time1 << endl;
            //clock_gettime(CLOCK_MONOTONIC, &begin1);
            std::vector<std::thread> worker;
            //worker.reserve(num_of_node-2);
            size_t i;
            for(i = 0; i<num_of_node-1;i++){
                worker.push_back(std::thread(&myRDMA::rdma_write_pagerank, &myrdma,i));
            }
            for(i=0;i<num_of_node-1;i++)
                worker[i].join();
            cout << "[INFO]START SEND - SUCCESS" << endl;
            //clock_gettime(CLOCK_MONOTONIC, &end1);
            time1 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
            //printf("%d: send 수행시간: %Lfs.\n", rank, time1);
        }
        else{
            MPI_Request request;
            //std::vector<MPI_Request> requests;
            //MPI_Bcast(recv1[0].data(), recv1[0].size(), MPI_DOUBLE, 0, MPI_COMM_WORLD);
            if(rank == 0){
                //cout << time1 << "s." <<endl;
                clock_gettime(CLOCK_MONOTONIC, &begin1);
                myrdma.rdma_recv_pagerank(0,buff_size);
                cout << "[INFO]START RECEIVE_RDMA - SUCCESS "<<endl;
                //est_buf[0] = recv1[0];
                clock_gettime(CLOCK_MONOTONIC, &end1);
                time1 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
                //cout << time1 << "s." << endl;
                rdma_time += time1;
                network_time += time1;
                //printf("%d: rdma_recv 수행시간: %Lfs.\n", rank, time1);
            }
            //MPI_Bcast(recv1[0].data(), recv1[0].size(), MPI_DOUBLE, 0, MPI_COMM_WORLD);
            //MPI_Allgather(&check, 1, MPI_INT, check1, 1, MPI_INT, MPI_COMM_WORLD);
            
            clock_gettime(CLOCK_MONOTONIC, &begin1);
            if(size > 1){
                if(rank == 0){
                //cout << "[INFO]START MPI_BCAST - SUCCESS "; 
                    for(size_t dest=1; dest<size; dest++){
                        MPI_Isend(recv_buffer_ptr, num_of_vertex, MPI_DOUBLE, dest, 32548, MPI_COMM_WORLD, &request);
                    }
                }
                else{
                    MPI_Irecv(recv_buffer_ptr, num_of_vertex, MPI_DOUBLE, 0, 32548, MPI_COMM_WORLD, &request);
                    MPI_Wait(&request, MPI_STATUS_IGNORE);
                }
            
            MPI_Allgather(&check, 1, MPI_INT, check1, 1, MPI_INT, MPI_COMM_WORLD);
            }
            //MPI_Bcast(recv_buffer_ptr, num_of_vertex, MPI_DOUBLE, 0, MPI_COMM_WORLD);
            clock_gettime(CLOCK_MONOTONIC, &end1);
           // MPI_Bcast(recv_buffer_ptr, num_of_vertex, MPI_DOUBLE, 0, MPI_COMM_WORLD);
            //clock_gettime(CLOCK_MONOTONIC, &end1);
            time1 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
            if(rank == 0){
                //cout << time1 << "s.\n" << endl;
                network_time += time1;
                mpi_time += time1;
                avg_compute_time += compute_time;
                printf("\nCOMPUTE PAGERANK:  %LFs.\n", compute_time);
                //printf("NETWORK(MPI+RDMA): %Lfs.\n", network_time);
                printf("NETWORK(RDMA): %Lfs.\n", network_time);
                printf("STEP %ld EXECUTION TIME: %Lfs.\n", step+1, compute_time + network_time);
                network_time = 0;
                compute_time = 0;
            }
        }
        clock_gettime(CLOCK_MONOTONIC, &end1);
        //time1 = (end1.tv_sec - begin1.tv_sec) + (end1.tv_nsec - begin1.tv_nsec) / 1000000000.0;
        //if(rank == 0)
         //   printf("%d: recv1 수행시간: %Lfs.\n", rank, time1);
        if(my_ip == node[0] && rank == 0)
            cout << "[INFO]DIFF: " <<diff << endl;
       
        //cout << recv1[0][0] << endl;
        if(diff < 1e-6 || recv1[0][0] >= 1){
            break;
        }
    }
    clock_gettime(CLOCK_MONOTONIC, &end2);
    long double time2 = (end2.tv_sec - begin2.tv_sec) + (end2.tv_nsec - begin2.tv_nsec) / 1000000000.0;

    //Sorting PageRank===============================================================================
    /*if(my_ip == node[0] && rank == 0){
        
        cout << "=====================================================" << endl;
        cout << "[INFO]SORTING PAGERANK." << endl;
        send[0][0] = send[0][0] - 1000000;
        //cout << "[INFO]SORTING PAGERANK VALUE." << endl;

        vector<pair<double,int>> result;
        for (int i = 0; i < num_of_vertex; ++i) {
            result.push_back(make_pair(send[0][i],i));
        }
        int topN = 5;
        partial_sort(result.begin(), result.begin() + topN, result.end(), greater<>());
        int important_idx = result[0].second;
        double important_value = result[0].first;

        for(int i=0;i<topN;i++){
            cout << "PR[" <<result[i].second<<"]: " << result[i].first <<endl;
        }
        
        //cout << "=====================================================" << endl;
    }*/
    //else if(my_ip != node[0] && rank == 0){
    //     cout << "=====================================================" << endl;
    //}
    //cout << "[INFO]Average Time: "<< sum_time3/30 << endl;
    //printf("[INFO]TOTAL EXECUTION TIME: %Lfs.\n", time2/30);
    ofstream outfile("output.txt");

    if(my_ip != node[0] && rank == 0){
        cout << "=====================================================" << endl;
        outfile << "=====================================================" << endl;

        recv1[0][0] = recv1[0][0] - 1;
        cout << "[INFO]SORTING PAGERANK VALUE." << endl;
        outfile << "[INFO]SORTING PAGERANK VALUE." << endl;

        vector<pair<double,int>> result;
        for (int i = 0; i < num_of_vertex; ++i) {
            result.push_back(make_pair(recv1[0][i],i));
        }
        
        int topN = 20;
        partial_sort(result.begin(), result.begin() + topN, result.end(), greater<>());
        int important_idx = result[0].second;
        double important_value = result[0].first;

        cout.precision(numeric_limits<double>::digits10);
        
        for(int i=0;i<topN;i++){
            cout << "PR[" <<result[i].second<<"]: " << result[i].first <<endl;
            outfile << "PR[" <<result[i].second<<"]: " << result[i].first <<endl;
        }
        
        cout << "=====================================================" << endl;
        outfile << "=====================================================" << endl;
       
        //cout << "[INFO]IMPORTANT VERTEX: " << important_idx << "\n[INFO]" << important_idx << "'S VALUE: "<<important_value << endl;
       // cout << "s = " <<round(sum1) << endl;
        //printf("총 수행시간: %Lfs.\n", time2);
    }
    if(rank == 0|| my_ip == node[0]){
        
        if(my_ip == node[0]){
            printf("[INFO]AVG EXECUTION TIME:   %LFs.\n", avg_compute_time/62);
            printf("[INFO]AVG MPI_TIME:  %Lfs.\n", mpi_time/62);
            printf("[INFO]AVG NETWORK TIME:     %Lfs.\n", rdma_time/62);
            printf("[INFO]TOTAL EXECUTION TIME: %Lfs.\n", time2);
            cout << "=====================================================" << endl;
        }
        else{
            printf("[INFO]AVG EXECUTION TIME:   %LFs.\n", avg_compute_time/62);
            printf("[INFO]AVG MPI_TIME:  %Lfs.\n", mpi_time/62);
            printf("[INFO]AVG NETWORK TIME:     %Lfs.\n", rdma_time/62);
            printf("[INFO]TOTAL EXECUTION TIME: %Lfs.\n", time2);
            cout << "=====================================================" << endl;
            outfile << "[INFO]AVG EXECUTION TIME:   " << avg_compute_time/62 << "s." << endl;
            outfile << "[INFO]AVG MPI_TIME:         " << mpi_time/62 << "s." << endl;
            outfile << "[INFO]AVG NETWORK TIME:     " << rdma_time/62 << "s." << endl;
            outfile << "[INFO]TOTAL EXECUTION TIME: " << time2 << "s." << endl;
            outfile << "=====================================================" << endl;

        }
    }

    outfile.close();

    MPI_Finalize();
    myrdma.exit_rdma();
}