#include <stdio.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <iostream>
#include <unistd.h>
#include <arpa/inet.h>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include "map"
#define PORT1 40100
#define BUF_SIZE 100
#define NAME_SIZE 20
#define NumOfServer 8

using namespace std;
class TCP{
    public:
        void Server();
        void Server_t();
        void Run(const char* iip);
        void Client(const char* iip);
        void Client_t(const char* iip);
        void check_ip(const char* iip);
        void send_msg(void * arg);
        void Send_Msg(const char* m,int ip);
        void Recv_Msg(int ip);
        void recv_msg(void * arg);
        int Scnt();
        int *connect_sock();
        int *client_sock();
        map<string, string> ReadRDMAInfo(int ip);
    private:
        const char* server[NumOfServer] = {"192.168.0.107", "192.168.0.108"};
        int sock;
        struct sockaddr_in serv_addr;
        struct sockaddr_in serv_adr, clnt_adr;
        int serv_sock, clnt_sock; 
        int clnt_adr_size;
        std::vector<std::thread> workers;
        char buffer[1048676] = {0};
        int valread;
        string result;
        string read_char;
};