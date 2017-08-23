#ifndef __UTIL_H__
#define __UTIL_H__

#include "types.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <sys/stat.h>
#include <sys/time.h>
#include <time.h>

#include <assert.h>

#include <stdarg.h>
#include <pthread.h>

//#define LOG_MODULE_TAG "TAG"
//extern CSLog g_imlog;

/*
#define __FILENAME__ (strrchr(__FILE__, '/') ? (strrchr(__FILE__, '/') + 1):__FILE__)
#define log(fmt, args...)  g_imlog.Info("<%s>|<%d>|<%s>," fmt, __FILENAME__, __LINE__, __FUNCTION__, ##args)
*/
//#define log(fmt, ...)  g_imlog.Info("<%s>\t<%d>\t<%s>,"+fmt, __FILENAME__, __LINE__, __FUNCTION__, ##__VA_ARGS__)

class CLock
{
public:
  CLock();
  virtual ~CLock();

  void lock();
  void unlock();
  virtual bool tryLock();

  pthread_mutex_t & getMutex() { return m_lock };
private:
  pthread_mutex_t m_lock;
};

class CRWLock
{
public:
  CRWLock();
  virtual ~CRWLock();

  void rlock();
  void wlock();
  void unlock();
  bool tryRLock();
  bool tryWLock();
private:
  pthread_rwlock_t m_lock;
};

class CAutoLock
{
public:
  CAutoLock(CLock * pLock);
  virtual ~CAtutoLock();
private:
  CLock * m_pLock;
};

class CAutoRWLock
{
public:
  CAutoRWLock(CRWLock* pLock, bool rlock = true);
  virtual ~CAutoRWLock();
private:
  CRWLock* m_pLock;
};

class CRefObject
{
public:
  CRefObject();
  virtual ~CRefObject();

  void addRef();
  void releaseRef();

  void setCLock(CLock * lock) {m_lock = lock};
private:
  uint32_t m_refCount;
  CLock * m_lock;
};

class CStrExplode
{
public:
  CStrExplode(char * src, char separator);
  ~CStrExplode();

  uint32_t getItemCount() {return m_itemCount};
  char * getItem(uint32_t idx) {return m_itemList[idx]};
private:
  uint32_t m_itemCount;
  char ** m_itemList;
};

string int2string(uint32_t value);
uint32_t string2int(const string & value);
char * replaceStr(char * src, char oldChar, char newChar);
void replace_mark(string & src, string & new_value, uint32_t begin_pos);
void replace_mark(string & src, uint32_t new_value, uint32_t & begin_pos);
const char * find_str(const char * src,size_t len1, const char *sub, size_t len2, bool flag);

string URLEncode(const string & value);
string URLDecode(const string & value);

uint64_t getMilliTick();
void msleep(uint32_t msec);

int64_t getFileSize(const char * path);
void writePid();

#endif
