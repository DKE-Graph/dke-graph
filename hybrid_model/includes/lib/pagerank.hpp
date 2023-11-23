#include <iostream>
#include <map>
#include <vector>
#include <stdio.h>
#include <algorithm>
#include <fstream>
#include <string>
#include <sstream>
#include <unistd.h>
#include <thread>
#include <stdlib.h>
#include <math.h>
#include <unordered_map>

#define df 0.85
#define buf_size1 1048676*30

using namespace std;
class Pagerank{
    public:
       void check_power_law_degree(vector<int>& num_outgoing);
       void create_graph(string path, string del, std::vector<std::vector<size_t>>* graph, vector<int>& num_outgoing);
       void create_vertex_weight(string path, string del, vector<int>& num_outgoing, 
                                int& num_of_vertex, int& start, int& end, int* nn,int num_of_node, 
                                int size,string* node, string my_ip, int rank, int* displs, 
                                int* recvcounts,vector<double> *send, vector<double> *recv1,string cmd);
       void create_graph_data(string path, string del);
       void create_sliced_graph(string path, string del, int start, int end, std::vector<std::vector<size_t>>& sliced_graph);
       void graph_partition(std::vector<std::vector<size_t>>* graph,std::vector<std::vector<size_t>>& sliced_graph,
                            vector<int>& num_outgoing, int num_of_vertex,
                             int& start, int& end, int* nn,int num_of_node, int size,string* node, string my_ip, int rank,
                             int* displs, int* recvcounts,vector<double> *send, vector<double> *recv1);
       void initial_pagerank_value();
       static void calc_pagerank_value(int start, int end, double x, double y);
       void run_pagerank(int iter);
       void gather_pagerank(string opcode);
       void scatter_pagerank();
       string max_pr();
       void init_connection(const char* ip, string server[], 
                            int number_of_server, int Port, int num_of_vertex);
       void print_pr();
       int get_num_of_vertex(); 
    private:
        vector<vector<size_t>> graph;
        vector<vector<size_t>> outgoing;
        string my_ip;
        int num_of_vertex;
        int num_of_server;
        string *node;
        string server_ip;
        int start1;
        int end1;
        double diff;
        vector<int> num_outgoing;
        bool add_arc(size_t from, size_t to, std::vector<std::vector<size_t>>* graph,vector<int>& num_outgoing);
        bool add_arc1(size_t from, size_t to,vector<int>& num_outgoing);
        template <class Vector, class T> bool insert_into_vector(Vector& v,
                                                             const T& t);
        vector<vector<size_t>> slice_graph(std::vector<std::vector<size_t>>& graph, int num_of_node, int size,string* node, string my_ip);
};