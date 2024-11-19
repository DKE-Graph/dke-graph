#include "threadpool.hpp"

// init static variables
bool ThreadPool::isInit;
vector<pthread_t *> ThreadPool::threadList;
vector<std::function<void()>> ThreadPool::jobQueue;
pthread_attr_t  ThreadPool::threadAttribute;
pthread_mutex_t ThreadPool::jobMutex;
pthread_cond_t ThreadPool::jobConditionVariable;

ThreadPool::ThreadPool()
{
    //loglevel = TRACE;
    //log(TRACE, "start", nullptr);
    cout << "start" << endl;
    isInit = true;

    pthread_attr_init(&threadAttribute);
    pthread_attr_setdetachstate(&threadAttribute, PTHREAD_CREATE_DETACHED);
    pthread_mutex_init(&jobMutex, nullptr);
    pthread_cond_init(&jobConditionVariable, nullptr);

    size_t i;
    pthread_t *tmp = nullptr;

    for(i=0 ; i<5 ; i++)
    {
        tmp = new pthread_t;
        threadList.push_back(tmp);

        pthread_create(tmp, &threadAttribute, worker, (void *)tmp);
    }
    //log(TRACE, "create thread variables and execute worker thread", nullptr);
    cout << "create thread variables and execute worker thread" <<endl;
    //log(TRACE, "end", nullptr);
    cout << "end" <<endl;
    return;
}

ThreadPool::ThreadPool(size_t size)
{
    //loglevel = TRACE;
    //log(TRACE, "start", nullptr);
    isInit = true;

    pthread_attr_init(&threadAttribute);
    pthread_attr_setdetachstate(&threadAttribute, PTHREAD_CREATE_DETACHED);
    pthread_mutex_init(&jobMutex, nullptr);
    pthread_cond_init(&jobConditionVariable, nullptr);

    size_t i;
    pthread_t *tmp = nullptr;

    for(i=0 ; i<size ; i++)
    {
        tmp = new pthread_t;
        threadList.push_back(tmp);

        pthread_create(tmp, &threadAttribute, worker, (void *)tmp);
    }
    //log(TRACE, "create thread variables and execute worker thread", nullptr);
    cout << "create thread variables and execute worker thread" << endl;
    //log(TRACE, "end", nullptr);
    return;
}

ThreadPool::~ThreadPool()
{
    //log(TRACE, "start", nullptr);
    int status;
    size_t i;
    for(i=0 ; i<threadList.size() ; i++)
    {   
        pthread_join(*threadList[i], (void**)&status);
        delete(threadList[i]);
    }

    jobQueue.clear();
    threadList.clear();

    pthread_cond_destroy(&jobConditionVariable);
    pthread_mutex_destroy(&jobMutex);
    pthread_attr_destroy(&threadAttribute);

    //log(TRACE, "end", nullptr);
    return;
}

bool ThreadPool::enqueueJob(std::function<void()> job)
{
    bool retval = false;

    jobQueue.push_back(std::move(job));

    pthread_cond_signal(&jobConditionVariable);

    return retval;
}

bool ThreadPool::stop()
{
    //log(TRACE, "start", nullptr);
    bool retval = false;
    
    pthread_mutex_lock(&jobMutex);
    if(isInit)
    {
        //log(TRACE, "size %d", jobQueue.size());
        printf("size %ld", jobQueue.size());
        jobQueue.clear();
        isInit = false;
        //log(TRACE, "execute stop command", nullptr);
        cout << "execute stop command" << endl;
    }
    else
    {
        //log(ERROR, "ThreadPool not initialized", nullptr);
        cout << "ThreadPool not initialized" << endl;
    }
    pthread_mutex_unlock(&jobMutex);

    //log(TRACE, "end", nullptr);
    cout << "end" << endl;
    return retval;
}

void *ThreadPool::worker(void *param)
{
    //void (*tmp)() = nullptr;
    std::function<void()> tmp;

    while(1==1)
    {
        pthread_mutex_lock(&jobMutex);
        if(jobQueue.size() == 0)
        {
            tmp = nullptr;
            pthread_cond_wait(&jobConditionVariable, &jobMutex);
        }
        else
        {
            tmp = jobQueue[0];
            //jobQueue.delIndex(0)
            jobQueue.erase(jobQueue.begin()+0);
        }
        pthread_mutex_unlock(&jobMutex);

        if(tmp != nullptr)
            tmp();
    }
    return nullptr;
}