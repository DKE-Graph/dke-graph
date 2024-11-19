#ifndef __THREAD__
#define __THREAD__

#include <unistd.h>
#include <pthread.h>
#include <vector>
#include <iostream>
#include <stdio.h>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <functional>
#include <mutex>
#include <queue>
#include <thread>

using namespace std;
class ThreadPool {
private:
    static bool isInit;
    static vector<pthread_t *> threadList;
    static vector<std::function<void()>> jobQueue;
    static pthread_attr_t threadAttribute;
    static pthread_mutex_t jobMutex;
    static pthread_cond_t jobConditionVariable;

    static void *worker(void *param);
public:
    ThreadPool();
    ThreadPool(size_t size);
    ~ThreadPool();

    bool enqueueJob(std::function<void()> job);
    bool stop();
};


#endif /* __THREAD__ */