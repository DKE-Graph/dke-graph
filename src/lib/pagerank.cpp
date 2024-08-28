#include "pagerank.hpp"
#include "../../includes/network/myRDMA.hpp"
#include "../../includes/network/tcp.hpp"
#include <numeric>
#include <time.h>
#include <omp.h>
//#include <mpi.h>

TCP tcp1;
myRDMA myrdma1;
Pagerank pagerank;
vector<int> sock_idx;
static std::mutex mutx;
vector<double> send_buffer[4];
vector<double> recv_buffer[4];
int n, n1;
vector<int> n2;
vector<int> nn;
//int number_outgoing = 0;

double min_max(double x, double min_x, double max_x){
    return (x-min_x)/(max_x-min_x);
}
double unit_step_func(double x){
    if(x < 0)
        return 0;
    else
        return 1;
}
double ReLU(double x, double y){
    if (x<y)
        return y;
    else
        return x;
}
vector<string> split(string str, char Delimiter) {
    istringstream iss(str);             
    string buffer;                     
    vector<string> result;
 
    while (getline(iss, buffer, Delimiter)) {
        result.push_back(buffer);   
    }
    return result;
}

double calculateStandardDeviation(vector<int>& num_outgoing, int n) {
    // 1. 평균 계산
    double sum = 0.0;
    double cnt = 0.0;
    double max = 0.0;
    double median_value;
    //vector<int> temp = num_outgoing;
    //sort(temp.begin(), temp.end());
    /*if (n % 2 == 0) {
        // 데이터 포인트의 개수가 짝수인 경우
        int middle1 = n / 2 - 1;
        int middle2 = n / 2;
        median_value = (num_outgoing[middle1] + num_outgoing[middle2]) / 2.0;
    } else {
        // 데이터 포인트의 개수가 홀수인 경우
        int middle = n / 2;
        median_value = num_outgoing[middle];
    }*/

    for (int i = 0; i < n; i++) {
        sum += num_outgoing[i];
        if(num_outgoing[i] > max)
            max = num_outgoing[i];
        if(num_outgoing[i] == 0)
            cnt++;
    }
    double mean = sum / n;
    //cout << "[INFO]AVG: "<< mean << endl;
    //cout << "[INFO]MAX: "<< max << endl;
    //cout << "[INFO]ZRO: "<< cnt << endl;
    //cout << "[INFO]MDV: " << median_value << endl;
    // 2. 각 데이터 포인트에서 평균을 뺀 값의 제곱 계산
    double squaredDifferences = 0.0;
    for (int i = 0; i < n; i++) {
        squaredDifferences += pow(num_outgoing[i] - mean, 2);
    }

    // 3. 분산 계산
    double variance = squaredDifferences / n;

    // 4. 표준 편차 계산 (분산의 제곱근)
    double stdDeviation = sqrt(variance);

    return stdDeviation;
}

template <class Vector, class T>
bool Pagerank::insert_into_vector(Vector& v, const T& t) {
    typename Vector::iterator i = lower_bound(v.begin(), v.end(), t);
    if (i == v.end() || t < *i) {
        v.insert(i, t);
        return true;
    } else {
        return false;
    }
}
bool Pagerank::add_arc(size_t from, size_t to, std::vector<std::vector<size_t>>* graph,vector<int>& num_outgoing) {
    bool ret = false;
    size_t max_dim = max(from, to);

    if ((*graph).size() <= max_dim) {
        max_dim = max_dim + 1;
        
        (*graph).resize(max_dim);
        //pagerank.outgoing.resize(max_dim);
        if (num_outgoing.size() <= max_dim) {
            num_outgoing.resize(max_dim,0);
        }
    }
    //pagerank.graph[to].push_back(from);
    //cout << pagerank.graph[to] << endl;

    ret = insert_into_vector((*graph)[to], from);

    if (ret) {
        num_outgoing[from]++;
        //if(num_outgoing[from] > max_edge){
        //    max_edge = num_outgoing[from];
        //}
    }

    return ret;
}
bool Pagerank::add_arc1(size_t from, size_t to,vector<int>& num_outgoing) {
    bool ret = true;
    size_t max_dim = max(from, to);

    if (num_outgoing.size() <= max_dim) {
        max_dim = max_dim + 1;
        
        num_outgoing.resize(max_dim,0);
        //pagerank.outgoing.resize(max_dim);
    }
    //pagerank.graph[to].push_back(from);
    //cout << pagerank.graph[to] << endl;

    //ret = insert_into_vector((*graph)[to], from);

    num_outgoing[from]++;
        //if(num_outgoing[from] > max_edge){
        //    max_edge = num_outgoing[from];

    return ret;
}
void Pagerank::check_power_law_degree(vector<int>& num_outgoing){
    vector<double> graph_deviation;
    int max_dimm = 0;
    for(int i =0; i<num_outgoing.size();i++){
        max_dimm = num_outgoing[i];
        if(graph_deviation.size() <= max_dimm)
            graph_deviation.resize(max_dimm+1.0,0);
        graph_deviation[max_dimm]++;
    }

    std::ofstream outputFile("graph_deviation.txt");
    if (outputFile.is_open()) {
        for (int i = 0; i < graph_deviation.size(); i++) {
            if(graph_deviation[i]!=0){
                outputFile << i << " " << graph_deviation[i] << std::endl;
                cout << i << ": " <<graph_deviation[i] << endl;
            }
        }
        outputFile.close(); // 파일 닫기
        std::cout << "데이터가 파일에 저장되었습니다." << std::endl;
    } else {
        std::cerr << "파일을 열 수 없습니다." << std::endl;
    } 
        /*for(int i =0; i<weighht.size();i++){
            cout << i << " " << weighht[i] << endl;
        }*/
}
void Pagerank::create_sliced_graph(string path, string del, int start, int end, std::vector<std::vector<size_t>>& sliced_graph, int rank){
    istream *infile;
    infile = new ifstream(path.c_str());
    string line;
    int temp;
    size_t line_num = 0;
    //std::vector<std::vector<size_t>>* slice_graph = new std::vector<std::vector<size_t>>();
	sliced_graph.resize(end-start);
    cout << end <<", " << start << endl;
    cout << end-start << endl;
    bool ret =false;
    size_t x;
    size_t y;

	if(infile){
        while(getline(*infile, line)) {
            string from, to;
            size_t pos;
            if(del == " ")
                pos = line.find(" ");
            else
                pos = line.find("\t");

            from = line.substr(0,pos);
            to = line.substr(pos+1);
            x = strtol(from.c_str(), NULL, 10);
            y = strtol(to.c_str(), NULL, 10);
            if(y >= start && y < end)
                ret = insert_into_vector(sliced_graph[y-start], x);
             line_num++;
            if(line_num % 50000000 == 0 && rank == 0){
                cout << "[INFO]READ "<< line_num<< " LINES." << endl;
            }
            
		}
	} 
    
    delete infile;

    //return slice_graph;

}
void Pagerank::create_vertex_weight(string path, string del, vector<int>& num_outgoing, 
                                int& num_of_vertex, int& start, int& end, int* nn,int num_of_node, 
                                int size,string* node, string my_ip, int rank, int* displs, 
                                int* recvcounts,vector<double> *send, vector<double> *recv1,string cmd, string alpha1)
{
    
    if(rank == 0){
        if(cmd == "1")
                cout << "[INFO]WEIGHT = EQUI-VERTEX" << endl;
        else if(cmd == "2")
                cout << "[INFO]WEIGHT = SQUARE ROOT" << endl;
        else if(cmd == "3")
                cout << "[INFO]WEIGHT = LOG" << endl;
        else if(cmd == "4" || cmd == "5")
                cout << "[INFO]WEIGHT = EQUI-EDGE" << endl;
        else if(cmd == "6")
                cout << "[INFO]WEIGHT = LOG + E" << endl;
        else if (cmd == "7")
            cout << "[INFO]DEWP-PRIME" << endl;
        else{
            cout << "[INFO]WEIGHT ERROR(1 ~ 5)" << endl;
            exit(0);
        }
    }
    istream *infile;
    infile = new ifstream(path.c_str());
    size_t line_num = 0;
    string line;
    int num_vertex = 0;
    int temp;
	int edge;

	if(infile){
        while(getline(*infile, line)) {
            string from, to;
            size_t pos;
            if(del == " ")
                pos = line.find(" ");
            else
                pos = line.find("\t");

            from = line.substr(0,pos);
            to = line.substr(pos+1);
            temp = max(strtol(from.c_str(), NULL, 10),strtol(to.c_str(), NULL, 10));
            if(num_vertex < temp)
                num_vertex = temp + 1;
            add_arc1(strtol(from.c_str(), NULL, 10),strtol(to.c_str(), NULL, 10),num_outgoing);
            
            line_num++;
            if(line_num % 50000000 == 0 && rank == 0){
                cout << "[INFO]READ "<< line_num<< " LINES." << endl;
            }
		}
	} 
    //num_of_vertex = num_vertex;
    edge = line_num;
    delete infile;
    
    double std_deviation = calculateStandardDeviation(num_outgoing, num_vertex);
    //cout << "[INFO] std deviation is " << std_deviation << endl;

    //cout << rank << " finish delete infile" << endl;
    int start_arr[num_of_node-1];
    start_arr[0] = 0;
    int end_arr[num_of_node-1];
    int start_arr_process[size-1];
    start_arr_process[0] = 0;
    int end_arr_process[size-1];
    //int temp = 0;
    size_t index = 0;
    int a,b;
    double xxxxx = 1.0/(num_of_node-1);
    struct timespec begin2, end2;
    //int edge_part = ceil((edge/(num_of_node-1)));
    //int vertex_part = ceil((num_of_vertex/(num_of_node-1))*argvv);
    //int part = ceil((edge+num_of_vertex)/(num_of_node-1));
    //cout << edge_part << endl;
    //long long buffer_size = num_of_vertex * sizeof(double);
    //long long buf_part = buffer_size/(num_of_node-1);
    //int ttt = 1;
    //cout << "ve: " << ve << endl;
    clock_gettime(CLOCK_MONOTONIC, &begin2);
    if (my_ip != "192.168.0.102"){
        double weight;
        vector<double> vertex_weight;
        double sum_weight = 0;
        double sum = 0;
        double z_score;
        double avg;
        double std;
        double median;
        double max;
        int c = 0;

        avg = round(std::max(10, edge/num_vertex));
        double alpha1_val = stod(alpha1);
        
        for(int i =0; i<num_vertex;i++){

            if(cmd == "1")
                weight =1;//log(num_outgoing[i]+1.0);//sqrt(num_outgoing[i]+1.0);//log(num_outgoing[i]+2.0);//log(log(num_outgoing[i] + 2.0)+1.0);//log(log(num_outgoing[i]+1.0)+1.0);//sqrt(sqrt(pow(num_outgoing[i],2.8))) + 1.0;//sqrt(sqrt(pow(num_outgoing[i],2.7)) + 1.0);// / max_edge;//log10(static_cast<long double>(max_edge));//1+log(static_cast<long double>(num_outgoing[i]+1.0)); // 로그에 1을 더하여 0으로 나누는 오류를 피합니다.
            else if(cmd == "2"){
                if(num_vertex == 2394385)
                    weight = sqrt(num_outgoing[i]+1.0);
                else
                    weight = sqrt(num_outgoing[i]);
            }
            else if(cmd == "3")
                weight = log(num_outgoing[i]+1.0);//pow(num_outgoing[i],1/1.7);
            else if(cmd == "4")
                weight = num_outgoing[i];//sqrt(num_outgoing[i]+6.0);
            else if(cmd == "5")
                weight = log(num_outgoing[i]+2.71);
            else if(cmd == "7"){
                //max = 2997469;
                //double alpha = 127;
                //z_score = num_outgoing[i]-round(avg);

                //size_t vm = num_outgoing[i] * sizeof(size_t);
                double n_diff = 0;
                double avg_reciprocal = 1.0 / avg;  

                double ratio = num_outgoing[i] * avg_reciprocal;
                n_diff = pow(ratio, alpha1_val) * sizeof(size_t);

                // exp 연산 최적화
                double diff = num_outgoing[i] - avg;
                double exp_val = exp(-5.0 * diff);
                double unit_step_val = 1.0 / (1.0 + exp_val);

                // sqrt와 weight 계산
                weight = 1.0 + sqrt(n_diff) * unit_step_val;

                
                
                //weight = 1 + (sqrt(num_outgoing[i]+(z_score*unit_step_func(num_outgoing[i]-max1)))-1)*unit_step_func(num_outgoing[i]-round(avg));// + sqrt(num_outgoing[i]+avg)*unit_step_func(0.95 - num_outgoing[i]/max);
                //weight = 1 + (sqrt(num_outgoing[i]+(num_outgoing[i]-round(avg))*unit_step_func(num_outgoing[i]-alpha))-1)*unit_step_func(z_score);
            }
            else{
                avg =14.2326;//35.253;//14.2326;//15.9151;//27.528;//14.2326;//35.253;//35.253;//14.2326;//14.2326;//14.2326;//14.2362;//35.253;// 27.528;//35.253;//14.2362;//35.253;//14.2362;//15.9151;//2;//15.9151;//6.54044;//5.57058;//11.092;//35.253;//14.2362;//35.253;//35.253;//2;//35.253;//14.2362;//14.2362;//2;//14.2362;//14.2362;//35.253;//+36;
                std = 2.98611;//2419.74;//87.0887;//36.0803;//2419.74;//30.4273;//99.915;//30.4273;//6.55653;//16.356;//2419.74;//36.0803;//2419.74;//2419.74;//99.915;//36.0803;//36.0803;//2419.74;
                max = 20293;
                median = 12;//3;//12;//3;//12;//0;
                double percent_80 = 20;//20;//37;//0;//20;//18;
                double percent_90 = 0;//33;//37;
                double after_avg = 127;//127;//78;//396;//46;//146;//69;//59;//88;//127;//260;//812;//188;//403;//146;//127;//68;//31;//9;  (403: twiter 0.9) (188: twitter 0.8) (812: twitter 0.95) (260: twitter 0.85) (127: lj 0.95) (88: lj 0.9)(69: lj 0.85)(59: lj 0.8)(146: wt 0.9)
                if(num_outgoing[i] <= round(avg))//pow(std,2)
                    //if(num_outgoing[i] > percent_90)
                        //weight = sqrt(num_outgoing[i]);
                    //else
                    weight = 1;//sqrt(num_outgoing[i]);// - (median - num_outgoing[i]) * (std / 2));
                else{
                    if(num_outgoing[i] <= after_avg)
                        weight = sqrt(num_outgoing[i]);
                    else{
                        z_score = num_outgoing[i]-round(avg);//avg)/std;
                    //if(z_score > 1)
                        weight = sqrt(num_outgoing[i]+z_score);//num_outgoing[i]);//((num_outgoing[i] * sqrt(z_score)) * sizeof(size_t)));//num_outgoing[i] * sqrt((num_outgoing[i]-median)));//sqrt((num_outgoing[i]+1)+(num_outgoing[i] - median));//num_outgoing[i]-median));
                    //else
                        //weight = sqrt(num_outgoing[i]);
                    }
                }
                
            }
          
            vertex_weight.push_back(weight);
            sum_weight += weight;
            //if(num_outgoing[i] == 0)
            //    c++;
        }
        for(int i =0; i<num_vertex;i++){
            vertex_weight[i] /= sum_weight;
          
        }
        cout << "Finish Fairness" << endl;
        //cout << c << endl;
        /*vector<double> weighht;
        int max_dimm = 0;
        for(int i =0; i<num_vertex;i++){
            vertex_weight[i] /= sum_weight;
            max_dimm = num_outgoing[i];
            if(weighht.size() < max_dimm)
                weighht.resize(max_dimm);
            weighht[num_outgoing[i]] = vertex_weight[i];
        }
        std::ofstream outputFile("output.txt");
        if (outputFile.is_open()) {
            for (int i = 0; i < weighht.size(); i++) {
                outputFile << i << " " << weighht[i] << std::endl;
            }
            outputFile.close(); // 파일 닫기
            std::cout << "데이터가 파일에 저장되었습니다." << std::endl;
        } else {
            std::cerr << "파일을 열 수 없습니다." << std::endl;
        } */  
        /*for(int i =0; i<weighht.size();i++){
            cout << i << " " << weighht[i] << endl;
        }*/
        cout << "doing something" << endl;
        for(int i =0; i<num_vertex;i++){
            sum += vertex_weight[i];
            if(sum >= xxxxx){
                //cout <<index << ": " <<sum << endl;
                end_arr[index] = i;
                sum = 0;
                if(index<num_of_node-1)
                    start_arr[index+1] = i;
                index++;
            }
            if(index == num_of_node-2)
                break;
        //printf("%llf\n", vertex_weight[i]);
        }
        end_arr[num_of_node-2] = num_vertex;
    }
    cout << "finish something" << endl;
    cout << "doing something" << endl;
    //cout << rank << " finish vertex weight" << endl;
    int div_num_of_vertex;
    if(my_ip != node[0]){
       for(int i=1;i<num_of_node;i++){
            if(node[i] == my_ip){
                div_num_of_vertex = end_arr[i-1] - start_arr[i-1];
                start = start_arr[i-1];
                end = end_arr[i-1];
            }
            cout << div_num_of_vertex << endl;
            cout << node[i] <<", " << my_ip << endl;
        }
        cout << rank << " start process vertex weight" << endl;
        //if(rank == 0){
            for(int i=0;i<num_of_node;i++){
                cout << i << endl;
                if(i == 0){
                    send[i].resize(div_num_of_vertex);
                    recv1[i].resize(num_vertex, 1/num_vertex);
                }
                else{
                    send[i].resize(1);
                    send[i].shrink_to_fit();
                    recv1[i].resize(1);
                    recv1[i].shrink_to_fit();
                }
                
            }
        cout << rank << " start process vertex weight" << endl;
        if(size > 1){
            vector<double> vertex_weight;
            double sum_weight = 0;
            double sum = 0;
            index = 0;
            int start_arr1[size];
            start_arr1[0] = start;
            int end_arr1[size];
            double div_weight = 1.0/size;

            for(int i =start; i<end;i++){
                double weight = sqrt(num_outgoing[i]);//sqrt(sqrt(pow(num_outgoing[i],2.7)) + 1.0);// / max_edge;//log10(static_cast<long double>(max_edge));//1+log(static_cast<long double>(num_outgoing[i]+1.0)); // 로그에 1을 더하여 0으로 나누는 오류를 피합니다.                vertex_weight.push_back(weight);
                vertex_weight.push_back(weight);
                sum_weight += weight;
            }
    
            for(int i=start; i<end;i++){
                vertex_weight[i-start] /= sum_weight;
            }
    
            for(int i =start; i<end;i++){
                sum += vertex_weight[i-start];
                if(sum >= div_weight){
                    end_arr1[index] = i;
                    sum = 0;
                    if(index<size)
                        start_arr1[index+1] = i;
                    index++;
                }
                if(index == size-1)
                    break;
            //printf("%llf\n", vertex_weight[i]);
            }
            end_arr1[size-1] = end;
            for(int i=0;i<size;i++){
                if(rank == i){
                    //div_num_of_vertex = end_arr[i-1] - start_arr[i-1];
                    start = start_arr1[i];
                    end = end_arr1[i];
                    
                }
                displs[i] = start_arr1[i]-start_arr1[0];
                recvcounts[i] = end_arr1[i] - start_arr1[i];
            }
        }
        
        //p_sliced_graph.resize(end-start);
        //p_sliced_graph = std::vector<std::vector<size_t>>(sliced_graph.begin() + start,sliced_graph.begin() + end + 1);
        //cout << "start, end: " << start <<", "<< end << endl;
        //sliced_graph = std::vector<std::vector<size_t>>((*graph).begin() + start,(*graph).begin() + end + 1);
    }
     else{
        for(int i=0;i<num_of_node-1;i++){
            int temp1 = end_arr[i] - start_arr[i];
            send[i].resize(num_vertex, 1/num_vertex);
            recv1[i].resize(temp1);
            nn[i] = temp1;
        }
        //num_outgoing.clear();
        //num_outgoing.shrink_to_fit();
        //delete graph;
    }
    clock_gettime(CLOCK_MONOTONIC, &end2);
    long double time2 = (end2.tv_sec - begin2.tv_sec) + (end2.tv_nsec - begin2.tv_nsec) / 1000000000.0;
    printf("[INFO]PARTITIONING EXECUTION TIME: %Lfs.\n", time2);
}
void Pagerank::create_graph(string path, string del,std::vector<std::vector<size_t>>* graph, vector<int>& num_outgoing){
    istream *infile;
    infile = new ifstream(path.c_str());
    size_t line_num = 0;
    string line;
    int edge;
	
	if(infile){
        while(getline(*infile, line)) {
            string from, to;
            size_t pos;
            if(del == " ")
                pos = line.find(" ");
            else
                pos = line.find("\t");

            from = line.substr(0,pos);
            to = line.substr(pos+1);
           
            add_arc(strtol(from.c_str(), NULL, 10),strtol(to.c_str(), NULL, 10),graph,num_outgoing);
            
            line_num++;
		}
	} 
    pagerank.num_of_vertex = (*graph).size();
    cout << line_num << endl;
    delete infile;
}
void Pagerank::graph_partition(std::vector<std::vector<size_t>>* graph,std::vector<std::vector<size_t>>& sliced_graph,
                             vector<int>& num_outgoing, int num_of_vertex,
                             int& start, int& end, int* nn, int num_of_node, int size,string* node, string my_ip, int rank,
                             int* displs, int* recvcounts, vector<double> *send, vector<double> *recv1)
{
    //std::vector<std::vector<size_t>>* slice_graph = new vector<vector<size_t>>();
    int start_arr[num_of_node-1];
    start_arr[0] = 0;
    int end_arr[num_of_node-1];
    int start_arr_process[size-1];
    start_arr_process[0] = 0;
    int end_arr_process[size-1];
    int temp = 0;
    size_t index = 0;
    int a,b;
    //int edge_part = ceil((edge/(num_of_node-1)));
    //int vertex_part = ceil((num_of_vertex/(num_of_node-1))*argvv);
    //int part = ceil((edge+num_of_vertex)/(num_of_node-1));
    //cout << edge_part << endl;
    //long long buffer_size = num_of_vertex * sizeof(double);
    //long long buf_part = buffer_size/(num_of_node-1);
    //int ttt = 1;
    //cout << "ve: " << ve << endl;
    if (my_ip != "1235"){
        vector<double> vertex_weight;
        double sum_weight = 0;
        double sum = 0;
        for(int i =0; i<num_of_vertex;i++){
            double weight = sqrt(num_outgoing[i]);//1;//sqrt(num_outgoing[i]+1.0);// / max_edge;//log10(static_cast<long double>(max_edge));//1+log(static_cast<long double>(num_outgoing[i]+1.0)); // 로그에 1을 더하여 0으로 나누는 오류를 피합니다.
            vertex_weight.push_back(weight);
            sum_weight += weight;
        }
    
        for(int i =0; i<num_of_vertex;i++){
            vertex_weight[i] /= sum_weight;
        }
    
        for(int i =0; i<num_of_vertex;i++){
            sum += vertex_weight[i];
            if(sum >= 0.25){
                end_arr[index] = i-1;
                sum = 0;
                if(index<num_of_node-1)
                    start_arr[index+1] = i-1;
                index++;
            }
            if(index == num_of_node-2)
                break;
        //printf("%llf\n", vertex_weight[i]);
        }
        end_arr[num_of_node-2] = num_of_vertex;
    }

    int div_num_of_vertex;
    if(my_ip != node[0]){
       for(int i=1;i<num_of_node;i++){
            if(node[i] == my_ip){
                div_num_of_vertex = end_arr[i-1] - start_arr[i-1];
                start = start_arr[i-1];
                end = end_arr[i-1];
            }
        }
        //if(rank == 0){
            for(int i=0;i<num_of_node;i++){
                if(i == 0){
                    send[i].resize(div_num_of_vertex);
                    recv1[i].resize(num_of_vertex, 1/num_of_vertex);
                }
                else{
                    send[i].resize(1);
                    send[i].shrink_to_fit();
                    recv1[i].resize(1);
                    recv1[i].shrink_to_fit();
                }
            }
        //}
        

        //delete graph;
       
         //=======================================================================
        /*temp =0;
        index=0;
        ttt=1;
        int num_edge = 0;
        for (int i = start; i < end; i++) {
            num_edge += num_outgoing[i];
        }
        start_arr_process[0] = start;
        for(size_t i =start; i<end;i++){
            temp += num_outgoing[i];
            if( temp+ttt*argvv >= num_edge/size+div_num_of_vertex/size*argvv){//+ ttt + (ttt*sizeof(double))> edge_part+vertex_part+buf_part){
            //cout << i << ", " << temp - num_outgoing[i] + ttt << endl;
                temp = num_outgoing[i];
                end_arr_process[index] = i;
                if(index<size)
                    start_arr_process[index+1] = i;
                ttt=0;
                index++;
            }
            ttt++;
            if(index == size-1)
                break;
        }
        end_arr_process[size-1] = div_num_of_vertex;
        if(my_ip == node[num_of_node-1]){
            end_arr_process[size-1] +=start_arr[3];
        }
        else if(my_ip == node[num_of_node-2]){
            end_arr_process[size-1] +=start_arr[2];
        }
        else if(my_ip == node[num_of_node-3]){
            end_arr_process[size-1] +=start_arr[1];
        }
        //=======================================================================
        for(int i=0;i<size;i++){
            if(rank == i){
                start = start_arr_process[i];
                end = end_arr_process[i];
            }
            displs[i] = start_arr_process[i]-start_arr_process[0];
            recvcounts[i] = end_arr_process[i] - start_arr_process[i];
            if(rank == 0){
                cout << recvcounts[i] << endl;
            }
        }
        /*if(my_ip == node[num_of_node-1]){
            start += end_arr[2];
            end += end_arr[2];
        }
        else if(my_ip == node[num_of_node-2]){
            start += end_arr[1];
            end += end_arr[1];
        }
        else if(my_ip == node[num_of_node-3]){
            start += end_arr[0];
            end += end_arr[0];
        }*/
        //=======================================================================
        //cout << rank << ", " <<div_num_of_vertex << ", " << start << ", " << end << endl;
        /*for(int i=0;i<size;i++){
            a = div_num_of_vertex/size*i;
            b = a + div_num_of_vertex/size;
            if(rank == i){
                start = a;
                end = b;
            }
            if(rank ==size-1 && rank == i){
                end = div_num_of_vertex;
            }
            displs[i] = a;
            recvcounts[i] = b-a;
            if(i ==size-1)
                recvcounts[i] = div_num_of_vertex-displs[i];
            //cout << "displs[" << i << "]: " <<displs[i] << endl;
            //cout << "recvcounts["<<i<<"]: " << recvcounts[i] << endl;
        }
        if(my_ip == node[num_of_node-1]){
            start += end_arr[2];
            end += end_arr[2];
        }
        else if(my_ip == node[num_of_node-2]){
            start += end_arr[1];
            end += end_arr[1];
        }
        else if(my_ip == node[num_of_node-3]){
            start += end_arr[0];
            end += end_arr[0];
        }*/

        if(size > 1){
            vector<double> vertex_weight;
            double sum_weight = 0;
            double sum = 0;
            index = 0;
            int start_arr1[size];
            start_arr1[0] = start;
            int end_arr1[size];
            double div_weight = 1.0/size;

            for(int i =start; i<end;i++){
                double weight = sqrt(num_outgoing[i]);//1;//sqrt(num_outgoing[i]+1.0);// / max_edge;//log10(static_cast<long double>(max_edge));//1+log(static_cast<long double>(num_outgoing[i]+1.0)); // 로그에 1을 더하여 0으로 나누는 오류를 피합니다.
                vertex_weight.push_back(weight);
                sum_weight += weight;
            }
    
            for(int i=start; i<end;i++){
                vertex_weight[i-start] /= sum_weight;
            }
    
            for(int i =start; i<end;i++){
                sum += vertex_weight[i-start];
                if(sum >= div_weight){
                    end_arr1[index] = i-1;
                    sum = 0;
                    if(index<size)
                        start_arr1[index+1] = i-1;
                    index++;
                }
                if(index == size-1)
                    break;
            //printf("%llf\n", vertex_weight[i]);
            }
            end_arr1[size-1] = end;
            for(int i=0;i<size;i++){
                if(rank == i){
                    //div_num_of_vertex = end_arr[i-1] - start_arr[i-1];
                    start = start_arr1[i];
                    end = end_arr1[i];
                    
                }
                displs[i] = start_arr1[i]-start_arr1[0];
                recvcounts[i] = end_arr1[i] - start_arr1[i];
            }
        }
        
        //p_sliced_graph.resize(end-start);
        //p_sliced_graph = std::vector<std::vector<size_t>>(sliced_graph.begin() + start,sliced_graph.begin() + end + 1);
        //cout << "start, end: " << start <<", "<< end << endl;
        sliced_graph = std::vector<std::vector<size_t>>((*graph).begin() + start,(*graph).begin() + end + 1);
    }
     else{
        for(int i=0;i<num_of_node-1;i++){
            int temp1 = end_arr[i] - start_arr[i];
            send[i].resize(num_of_vertex, 1/num_of_vertex);
            recv1[i].resize(temp1);
            nn[i] = temp1;
        }
        //num_outgoing.clear();
        //num_outgoing.shrink_to_fit();
        //delete graph;
    }
     /*for(size_t i=0;i<num_of_vertex;i++){
        temp += num_outgoing[i];
        if( temp+ttt*argvv >= edge_part+vertex_part){//+ ttt + (ttt*sizeof(double))> edge_part+vertex_part+buf_part){
            //cout << i << ", " << temp - num_outgoing[i] + ttt << endl;
            temp = num_outgoing[i];
            end_arr[index] = i;
            if(index<num_of_node-1)
                start_arr[index+1] = i;
            //cout << "===========================" << endl;
            //cout << "start["<<index<<"]: " << start_arr[index] <<endl;
            //cout << "end["<<index<<"]: " << end_arr[index] <<endl;
            ttt=0;
            index++;
        }
        ttt++;
        if(index == num_of_node-2)
            break;
    }
    //cout << "===========================" << endl;
    end_arr[num_of_node-2] = num_of_vertex;*/

    //===============================================================================
    
    //cout << "start["<<index<<"]: " << start_arr[index] <<endl;
    //cout << "end["<<index<<"]: " << end_arr[index] <<endl;
    //cout << "===========================" << endl;
    
    /*if(my_ip != node[0]){
        //=======================================================================
        /*temp =0;
        index=0;
        ttt=1;
        int num_edge = 0;
        for (int i = start; i < end; i++) {
            num_edge += num_outgoing[i];
        }
        start_arr_process[0] = start;
        for(size_t i =start; i<end;i++){
            temp += num_outgoing[i];
            if( temp+ttt*argvv >= num_edge/size+div_num_of_vertex/size*argvv){//+ ttt + (ttt*sizeof(double))> edge_part+vertex_part+buf_part){
            //cout << i << ", " << temp - num_outgoing[i] + ttt << endl;
                temp = num_outgoing[i];
                end_arr_process[index] = i;
                if(index<size)
                    start_arr_process[index+1] = i;
                ttt=0;
                index++;
            }
            ttt++;
            if(index == size-1)
                break;
        }
        end_arr_process[size-1] = div_num_of_vertex;
        if(my_ip == node[num_of_node-1]){
            end_arr_process[size-1] +=start_arr[3];
        }
        else if(my_ip == node[num_of_node-2]){
            end_arr_process[size-1] +=start_arr[2];
        }
        else if(my_ip == node[num_of_node-3]){
            end_arr_process[size-1] +=start_arr[1];
        }
        //=======================================================================
        for(int i=0;i<size;i++){
            if(rank == i){
                start = start_arr_process[i];
                end = end_arr_process[i];
            }
            displs[i] = start_arr_process[i]-start_arr_process[0];
            recvcounts[i] = end_arr_process[i] - start_arr_process[i];
            if(rank == 0){
                cout << recvcounts[i] << endl;
            }
        }
        /*if(my_ip == node[num_of_node-1]){
            start += end_arr[2];
            end += end_arr[2];
        }
        else if(my_ip == node[num_of_node-2]){
            start += end_arr[1];
            end += end_arr[1];
        }
        else if(my_ip == node[num_of_node-3]){
            start += end_arr[0];
            end += end_arr[0];
        }*/
        //=======================================================================
        //cout << rank << ", " <<div_num_of_vertex << ", " << start << ", " << end << endl;
        /*for(int i=0;i<size;i++){
            a = div_num_of_vertex/size*i;
            b = a + div_num_of_vertex/size;
            if(rank == i){
                start = a;
                end = b;
            }
            if(rank ==size-1 && rank == i){
                end = div_num_of_vertex;
            }
            displs[i] = a;
            recvcounts[i] = b-a;
            if(i ==size-1)
                recvcounts[i] = div_num_of_vertex-displs[i];

            //cout << "displs[" << i << "]: " <<displs[i] << endl;
            //cout << "recvcounts["<<i<<"]: " << recvcounts[i] << endl;
        }
        if(my_ip == node[num_of_node-1]){
            start += end_arr[2];
            end += end_arr[2];
        }
        else if(my_ip == node[num_of_node-2]){
            start += end_arr[1];
            end += end_arr[1];
        }
        else if(my_ip == node[num_of_node-3]){
            start += end_arr[0];
            end += end_arr[0];
        }*/
        //send[0][0] = div_num_of_vertex;
        //cout << "start, end: " << start <<", "<< end << endl;
    //}
    //else{
        /*for(int i=0;i<num_of_node-1;i++){
            int temp1 = end_arr[i]-start_arr[i];
            send[i].resize(num_of_vertex, 1/num_of_vertex);
            recv1[i].resize(temp1);
            nn[i] = temp1;
        }*/
    //}
    
    //std::vector<std::vector<size_t>>().swap(graph);
    /*int div_num_of_vertex = num_of_vertex/(num_of_node-1);    
    if(my_ip == node[num_of_node-1])
        div_num_of_vertex = num_of_vertex - num_of_vertex/(num_of_node-1)*3;

    //cout << "start "<< endl;
    if(my_ip != node[0]){
        //cout << "div_num_of_vertex: " <<div_num_of_vertex << endl;
        for(int i=0;i<size;i++){
            a = div_num_of_vertex/size*i;
            b = a + div_num_of_vertex/size;
            if(rank == i){
                start = a;
                end = b;
            }
            if(rank ==size-1 && rank == i){
                end = div_num_of_vertex;
            }
            displs[i] = a;
            recvcounts[i] = b-a;
            if(i ==size-1)
                recvcounts[i] = div_num_of_vertex-displs[i];

            //cout << "displs[" << i << "]: " <<displs[i] << endl;
            //cout << "recvcounts["<<i<<"]: " << recvcounts[i] << endl;
        }
        if(my_ip == node[num_of_node-1]){
            start += (num_of_vertex/(num_of_node-1))*3;
            end += (num_of_vertex/(num_of_node-1))*3;
        }
        else if(my_ip == node[num_of_node-2]){
            start += num_of_vertex/(num_of_node-1)*2;
            end += num_of_vertex/(num_of_node-1)*2;
        }
        else if(my_ip == node[num_of_node-3]){
            start += num_of_vertex/(num_of_node-1);
            end += num_of_vertex/(num_of_node-1);
        }
         //cout << "start, end: " << start <<", "<< end << endl;
        for(int i=0;i<num_of_node;i++){
            send[i].resize(div_num_of_vertex);
            recv1[i].resize(num_of_vertex, 1/num_of_vertex);
        }
    }
    else{
        for(int i=0;i<num_of_node;i++){
            send[i].resize(num_of_vertex, 1/num_of_vertex);
            recv1[i].resize(div_num_of_vertex);
            nn[i] = div_num_of_vertex;
        }
        int x = num_of_vertex - num_of_vertex/(num_of_node-1)*3;
        recv1[num_of_node-2].resize(x);

        nn[num_of_node-2] = x;
    }*/
}
vector<vector<size_t>> Pagerank::slice_graph(std::vector<std::vector<size_t>>& graph, int num_of_node, int size, string* node, string my_ip){
    int recvcounts[size];
    int displs[size]; 
    int nn[num_of_node];
    int start_arr[num_of_node-1];
    start_arr[0] = 0;
    int end_arr[num_of_node-1];
    int start_arr_process[size-1];
    start_arr_process[0] = 0;
    int end_arr_process[size-1];
    int temp = 0;
    size_t index = 0;
    int start, end;


    vector<double> vertex_weight;
    double sum_weight = 0;
    double sum = 0;
    for(int i =0; i<pagerank.num_of_vertex;i++){
        double weight = sqrt(pagerank.num_outgoing[i]+1.0);// / max_edge;//log10(static_cast<long double>(max_edge));//1+log(static_cast<long double>(num_outgoing[i]+1.0)); // 로그에 1을 더하여 0으로 나누는 오류를 피합니다.
        vertex_weight.push_back(weight);
        sum_weight += weight;
    }
    
    for(int i =0; i<pagerank.num_of_vertex;i++){
        vertex_weight[i] /= sum_weight;
    }
    
    for(int i =0; i<pagerank.num_of_vertex;i++){
        sum += vertex_weight[i];
        if(sum >= 0.25){
            end_arr[index] = i-1;
            sum = 0;
            if(index<num_of_node-1)
                start_arr[index+1] = i-1;
            index++;
        }
        if(index == num_of_node-2)
            break;
        //printf("%llf\n", vertex_weight[i]);
    }
    end_arr[num_of_node-2] = pagerank.num_of_vertex;

    int div_num_of_vertex;
    for(int i=1;i<num_of_node;i++){
        if(node[i] == my_ip){
            div_num_of_vertex = end_arr[i-1] - start_arr[i-1];
            start = start_arr[i-1];
            end = end_arr[i-1];
        }
    }
    
    return std::vector<std::vector<size_t>>(graph.begin() + start,graph.begin() + end + 1);
}
void Pagerank::create_graph_data(string path, string del){
    //cout << "Creating graph about  "<< path<<"..."  <<endl;
    pagerank.num_of_vertex = num_of_vertex;
    istream *infile;

    infile = new ifstream(path.c_str());
    size_t line_num = 0;
    string line;
	
	if(infile){
        while(getline(*infile, line)) {
            string from, to;
            size_t pos;
            if(del == " ")
                pos = line.find(" ");
            else{
                pos = line.find("\t");
            }
            from = line.substr(0,pos);
            to = line.substr(pos+1);
           
            //add_arc(strtol(from.c_str(), NULL, 10),strtol(to.c_str(), NULL, 10));
            line_num++;
            //if(line_num%5000000 == 0)
                //cerr << "Create " << line_num << " lines" << endl;
		}
	} 
    else {
		cout << "Unable to open file" <<endl;
        exit(1);
	}

    pagerank.num_of_vertex = pagerank.graph.size();
    //cerr << "partition number_outgoing: " << line_num/4 << endl;
    //cerr << "Create " << line_num << " lines, "
    //     << pagerank.num_of_vertex << " vertices graph." << endl;
    
    //cerr << "----------------------------------" <<endl;
    
    int n3 = 0;
    int number_outgoing = line_num/3 + 1;
    for(int i=0;i<pagerank.num_of_vertex;i++){

        n3 += pagerank.graph[i].size();
        if(n3 >= number_outgoing){
            n2.push_back(i);
            n3 = 0;
        }
        
    }
    n2[1] = n2[1] -200000;
    int xx = pagerank.num_of_vertex - (n2[1]+n2[0]);
    n2.push_back(xx/4 * 2.9);
    n2.push_back(pagerank.num_of_vertex - (n2[1]+n2[0]+n2[2]));
    for(int i=0;i<n2.size();i++){
        cout << n2[i] << endl;
    }
    
    delete infile;
}

void Pagerank::initial_pagerank_value(){
    cout << "init pagerank value" << endl;
   
    /*n = pagerank.num_of_vertex/(pagerank.num_of_server-1);
    
    n1 = pagerank.num_of_vertex - n*(pagerank.num_of_server-2);*/
    int init = 0;
    for(int i=1;i<4;i++){
        if(pagerank.my_ip == pagerank.node[i]){
            n = n2[i-1] - init;
            
        }
        nn.push_back(n2[i-1] - init);
        init = n2[i-1];
        
    }
    if(pagerank.my_ip == pagerank.node[pagerank.num_of_server-1]){
        n = pagerank.num_of_vertex - init;
    }
    nn.push_back(pagerank.num_of_vertex - init);
    cout << "n: " << n << endl;
    if(pagerank.my_ip == pagerank.node[0]){
        send_buffer[0].resize(pagerank.num_of_vertex);
    }
    else{
        send_buffer[0].resize(n);
    }
    
    //recv_buffer[0].resize(pagerank.num_of_vertex,1/pagerank.num_of_vertex);
    
    init=0;
    for(int i=1;i<pagerank.num_of_server-1;i++){
        if(pagerank.my_ip == pagerank.node[i]){
            pagerank.start1 = init;
            pagerank.end1 = n2[i-1];
        }
        init = n2[i-1];
        
    }
    if(pagerank.my_ip == pagerank.node[pagerank.num_of_server-1]){
        pagerank.start1 = init;
        pagerank.end1 = pagerank.num_of_vertex;
    }
    cout << pagerank.start1 << " " <<pagerank.end1 <<endl;

    cout << "Done" <<endl;
}

void Pagerank::calc_pagerank_value(int start, int end, double x, double y){
    const int num_of_vertex = pagerank.num_of_vertex;
    double df_inv = 1.0 - df;
    double inv_num_of_vertex = 1.0 / num_of_vertex;
    const vector<vector<size_t>>& graph = pagerank.graph;
    const vector<int>& num_outgoing = pagerank.num_outgoing;
    
    double* recv_buffer_ptr = recv_buffer[0].data();    
    double* send_buffer_ptr = send_buffer[0].data();
    
    for(size_t i=start;i<end;i++){
        double tmp = 0.0;
        const size_t graph_size = graph[i].size();
        const size_t* graph_ptr = graph[i].data();

        for(size_t j=0; j<graph_size; j++){
            const size_t from_page = graph_ptr[j];
            const double inv_num_outgoing = 1.0 / num_outgoing[from_page];

            tmp += recv_buffer_ptr[from_page] * inv_num_outgoing;
        }
        send_buffer_ptr[i-start] = (tmp + x * inv_num_of_vertex) * df + df_inv * inv_num_of_vertex;
    }
    
}


void Pagerank::run_pagerank(int iter){
    cout << "progressing..." << endl;
    
    vector<double> prev_pr;
    size_t step;
    pagerank.diff = 1;
    string my_ip = pagerank.my_ip;
    string server_ip = pagerank.server_ip;
    int start = pagerank.start1;
    int end1 = pagerank.end1;
    int num_of_vertex = pagerank.num_of_vertex;
    double diff=1;
    double dangling_pr = 0.0;
    const vector<int>& num_outgoing = pagerank.num_outgoing;
    double* recv_buffer_ptr = recv_buffer[0].data();    
    double* send_buffer_ptr = send_buffer[0].data();
    struct timespec begin, end;
    long double time;

    for(step =0; step < iter ;step++){
        //clock_gettime(CLOCK_MONOTONIC, &begin);
        cout <<"====="<< step+1 << " step=====" <<endl;
        
        dangling_pr = 0.0;
        if(step!=0) {
            if(my_ip != server_ip){
                for (size_t i=0;i<num_of_vertex;i++) {
                    if (num_outgoing[i] == 0)
                        dangling_pr += recv_buffer_ptr[i];   
                }
            }
            else{
                diff = 0;
                for (size_t i=0;i<num_of_vertex;i++) 
                    diff += fabs(prev_pr[i] - send_buffer_ptr[i]);
                pagerank.diff = diff;
            }
            
        }
        clock_gettime(CLOCK_MONOTONIC, &begin);
        if(my_ip != server_ip)
            Pagerank::calc_pagerank_value(start,end1,dangling_pr,0.0);
        else
            prev_pr = send_buffer[0];
        clock_gettime(CLOCK_MONOTONIC, &end);
        time = (end.tv_sec - begin.tv_sec) + (end.tv_nsec - begin.tv_nsec) / 1000000000.0;
        printf("calc 수행시간: %Lfs.\n", time);

        //cout << "finish calc" <<endl;
        
      
        //clock_gettime(CLOCK_MONOTONIC, &begin);
        
        Pagerank::gather_pagerank("send");

        //cout << "finish gath" << endl;
        //clock_gettime(CLOCK_MONOTONIC, &end);
        //time = (end.tv_sec - begin.tv_sec) + (end.tv_nsec - begin.tv_nsec) / 1000000000.0;
        //printf("gath 수행시간: %Lfs.\n", time);
        //cout << "hello" <<endl;
        //clock_gettime(CLOCK_MONOTONIC, &begin); 
            //thread scatter = thread(&Pagerank::scatter_pagerank,Pagerank());
        Pagerank::scatter_pagerank();

        //cout << "finish scat" << endl;

       
        if(my_ip == server_ip)
            cout << "diff: " <<diff << endl;
        //printf("step 수행시간: %Lfs.\n", time);
        if(diff < 0.00001 || recv_buffer_ptr[0] > 1){
            break;
        }
        /*clock_gettime(CLOCK_MONOTONIC, &end);
        time = (end.tv_sec - begin.tv_sec) + (end.tv_nsec - begin.tv_nsec) / 1000000000.0;
        printf("step 수행시간: %Lfs.\n", time);*/

    }
    
}

string Pagerank::max_pr(){
    int important = 0;
    double important_pr = recv_buffer[0][0]-1;

    for (int i = 1; i < pagerank.num_of_vertex; ++i) {
        if (recv_buffer[0][i] > important_pr) {
            important = i;
            important_pr = recv_buffer[0][i];
        }
    }

    stringstream ss;
    ss << "important page is " << important << " and value is " << important_pr;
    return ss.str();
}

void Pagerank::init_connection(const char* ip, string server[], int number_of_server, int Port, int num_of_vertex)
{
    myrdma1.initialize_rdma_connection_vector(ip,server,number_of_server,Port,send_buffer,recv_buffer,num_of_vertex);
    myrdma1.create_rdma_info(send_buffer, recv_buffer);
    myrdma1.send_info_change_qp();

    string str_ip(ip);

    pagerank.my_ip = str_ip; 
    pagerank.num_of_server = number_of_server;
    pagerank.diff = 1;
    pagerank.node = server;
    pagerank.server_ip = server[0];

    /*for(int i=1;i<number_of_server;i++){
        if(ip == server[i]){
            pagerank.start1 = pagerank.num_of_vertex/(number_of_server-1)*(i-1);
            pagerank.end1 = pagerank.start1 + pagerank.num_of_vertex/(number_of_server-1);
        }
        if(ip == server[number_of_server-1]){
            pagerank.end1 = pagerank.num_of_vertex;
        }
    }
    cout << pagerank.start1 << " " <<pagerank.end1 <<endl;*/
}
void fill_send_buffer(int num_of_server, int index){
    int size = n;
    
    for(int i=0;i<num_of_server-1;i++){
        size = nn[i];
        send_buffer[0].insert(send_buffer[0].end(),recv_buffer[i].begin(),recv_buffer[i].begin()+size);
    }   
 
}
void send_pagerank(int num_of_server){
    for(size_t i = 0; i<num_of_server-1;i++)
        int a= 1;
        //myrdma1.rdma_write_pagerank(i);
}
void Pagerank::gather_pagerank(string opcode){
    if(pagerank.my_ip == pagerank.server_ip){
        myrdma1.recv_t("send");
        cout << "recv success" << endl;
        send_buffer[0].clear();

        fill_send_buffer(pagerank.num_of_server, pagerank.num_of_server-2);

        if(pagerank.diff < 0.00001)
            send_buffer[0][0] += 1; 
            
        fill(&send_buffer[1], &send_buffer[pagerank.num_of_server-1], send_buffer[0]);
       
    }
    else{
        //myrdma1.rdma_write_vector(send_buffer[0],0);
        cout << "send success" << endl;
    } 
}


void Pagerank::scatter_pagerank(){
        if(pagerank.my_ip == pagerank.server_ip){
            send_pagerank(pagerank.num_of_server);
            cout << "send success" << endl;
        }
        else{
            myrdma1.rdma_recv_pagerank(0, 1);
            cout << "recv success" << endl;
        }
    
}


void Pagerank::print_pr(){
    size_t i;
    double sum = 0;
    double sum1 = accumulate(recv_buffer[0].begin(), recv_buffer[0].end(), -1.0);
    cout.precision(numeric_limits<double>::digits10);
    for(i=pagerank.num_of_vertex-200;i<pagerank.num_of_vertex;i++){
        cout << "pr[" <<i<<"]: " << recv_buffer[0][i] <<endl;
        sum += recv_buffer[0][i];
    }
    cerr << "s = " <<round(sum1) << endl;
}

int Pagerank::get_num_of_vertex(){
    return pagerank.num_of_vertex;
}