#include "util.h"
#include <sstream>
using namespace std;

// CSLog g_imlog = CSLog(LOG_MODULE_TAG);

/***************************************************************
 *  CLock
 ****************************************************************/
CLock::CLock()
{
  pthread_mutex_init(&m_lock, NULL);
}

CLock::~CLock()
{
  pthread_mutex_destroy(&m_lock);
}

void CLock::lock()
{
  pthread_mutex_lock(&m_lock);
}

void CLock::unlock()
{
  pthread_mutex_unlock(&m_lock);
}

bool CLock::tryLock()
{
  return pthread_mutex_trylock(&m_lock) == 0;
}

/***************************************************************
 *  CRWLock
 ****************************************************************/
CRWLock::CRWLock()
{
  pthread_rwlock_init(&m_lock, NULL);
}

CRWLock::~CRWLock()
{
  pthread_rwlock_destroy(&m_lock);
}

void CRWLock::rlock()
{
  pthread_rwlock_rdlock(&m_lock);
}

void CRWLock::wlock()
{
  pthread_rwlock_wrlock(&m_lock);
}

bool CRWLock::tryRLock()
{
  return pthread_rwlock_tryrdlock(&m_lock) == 0;
}

bool CRWLock::tryWLock()
{
  return pthread_rwlock_trywrlock(&m_lock) == 0;
}

void CRWLock::unlock()
{
  return pthread_rwlock_unlock(&m_lock);
}

/***************************************************************
 *  CAutoLock
 ****************************************************************/
CAutoLock::CAutoLock(CLock * pLock)
{
  m_pLock = pLock;
  if (NULL != m_pLock) {
    m_pLock->lock();
  }
}

CAutoLock::~CAutoLock()
{
  if (NULL != m_pLock) {
    m_pLock->unlock();
    m_pLock = NULL;
  }
}

/***************************************************************
 *  CAutoLock
 ****************************************************************/
CAutoRWLock::CAutoRWLock(CRWLock * pLock, bool rlock = true)
{
  m_pLock = pLock;
  if (NULL != m_pLock) {
    if (rlock) {
      m_pLock->rlock();
    } else {
      m_pLock->wlock();
    }
  }
}

CAutoRWLock::~CAutoRWLock()
{
  if (NULL != m_pLock) {
    m_pLock->unlock();
    m_pLock = NULL;
  }
}

/***************************************************************
 * CRefObject
 ****************************************************************/
CRefObject::CRefObject()
{
  m_refCount = 1;
  m_lock = NULL;
}

CRefObject::~CRefObject()
{

}

void CRefObject::addRef()
{
  if (NULL != m_lock) {
    m_lock->lock();
    ++m_refCount;
    m_lock->unlock();
  } else {
    ++m_refCount;
  }
}

// FIXME
void CRefObject::releaseRef()
{
  if (NULL != m_lock) {
    m_lock->lock();
    --m_refCount;
    if (0 == m_refCount) {
      delete this;
      return ;
    }
    m_lock->unlock();
  } else {
    --m_refCount;
    if (0 == m_refCount) {
      delete this;
    }
  }
}

/***************************************************************
 * CStrExplode
 ****************************************************************/
// TODO
CStrExplode::CStrExplode(char * src, char separator)
{
  m_itemCount = 1;

  size_t size = strlen(src);
  char * pos = src;
  char * start = NULL;
  char * end = NULL;
  while((separator == *pos) && *pos) {
    ++pos;
  }

  if (*pos) {
    start = pos;
  } else {
    m_itemCount = 0;
    return ;
  }

  pos = src + (size - 1);
  while((separator == *pos) && pos != start) {
    --pos;
  }

  end = pos;
  pos = start;
  char * trim = src;
  while(pos != end) {
    if (separator == *pos) {
      *trim++ = *pos++;
      ++m_itemCount;
      while((separator == *pos) && pos != end) {
        ++pos;
      }
    } else {
      *trim++ = *pos++;
    }
  }
  *trim = *end;

  uint32_t idx = 0;
  m_itemList = new char*[m_itemCount];
  start = pos = src;
  while(pos != trim) {
    if (separator == *pos) {
      uint32_t len = pos - start;
      m_itemList[idx] = new char[len + 1];
      strncpy(m_itemList[idx], start, len);
      m_itemList[idx][len] = 0;
      ++idx;
      start = pos + 1;
    }
    ++pos;
  }
  uint32_t len = pos - start;
  m_itemList[idx] = new char[len + 1];
  strncpy(m_itemList[idx], start, len);
  m_itemList[idx][len] = 0;

}

CStrExplode::~CStrExplode()
{
  for(uint32_t i = 0; i < m_itemCount; ++i) {
    delete [] m_itemList[i];
  }
  delete [] m_itemList;
}

string int2string(uint32_t value)
{
  stringstream ss;
  ss << value;
  return ss.str();
}

uint32_t string2int(const string & value)
{
  return (uint32_t)atoi(value.c_str());
}

char * replaceStr(char * src, char oldChar, char newChar)
{
  if (NULL == src) {
    return NULL;
  }

  char * pos = src;
  while(*pos) {
    if(oldChar == *pos) {
      *pos = newChar;
    }
    ++pos;
  }
  return src;
}

void replace_mark(string & src, string & new_value, uint32_t & begin_pos)
{
  string::size_type pos = src.find('?', begin_pos);
  if (string::npos == pos) {
    return ;
  }
  string prime_new_value = "'" + new_value + "'";
  src.replace(pos, 1, prime_new_value);
  begin_pos = pos + prime_new_value.size();
}

void replace_mark(string & src, uint32_t new_value, uint32_t & begin_pos)
{
  stringstream ss;
  ss << new_value;
  string str_new_value = ss.str();
  string::size_type pos = src.find('?', begin_pos);
  if (string::npos == pos) {
    return ;
  }
  src.replace(pos, 1, str_new_value);
  begin_pos = pos + str_new_value.size();
}

const char * find_str(const char * src,size_t len1, const char *sub, size_t len2, bool flag)
{
  if (NULL == src || NULL == sub || 0 >= len1 || 0 >= len2) {
    return NULL;
  }
  if (len2 > len1) {
    return NULL;
  }
  if (len1 == len2) {
    if (0 == memcmp(src, sub, len1)) {
      return src;
    } else {
      return NULL;
    }
  }

  const char * pos = NULL;
  if (flag) {
    for (int i = 0; i < len1 - len2; ++i) {
      pos = src + i;
      if (0 == memcmp(pos, sub, len2)) {
        return pos;
      }
    }
  } else {
    for (int i = len1 - len2; i >= 0; --i) {
      pos = src + i;
      if (0 == memcmp(pos, sub, len2)) {
        return pos;
      }
    }
  }

  return NULL;
}

// int => hex
unsigned char toHex(const unsigned char &x)
{
  return x > 9 ? 'A' + x - '9' : '0' + x;
}

// hex char => int
unsigned char fromHex(const unsigned char &x)
{
  return isdigit(x) ? x - '0' : x - 'A' + 10;
}

string URLEncode(const string & value)
{
  string out;
  for(uint32_t idx = 0; idx < value.size(); ++idx) {
    char buf[4] = {0};
    if (isalnum((unsigned char)value[idx])) {
      buf[0] = value[idx];
    } else {
      buf[0] = '%';
      buf[1] = toHex((unsigned char)value[idx] >> 4);
      buf[2] = toHex((unsigned char)value[idx] % 16);
    }
    out += (char *)buf;
  }
  return out;
}

string URLDecode(const string & value)
{
  string out;
  for(uint32_t idx = 0; idx < value.size(); ++idx) {
    unsigned char ch;
    if ('%' == value[idx]) {
      ch = fromHex(value[idx + 1] << 4);
      ch |= fromHex(value[idx + 2]);
      idx += 2;
    } else {
      ch = value[idx];
    }
    out += (char)ch;
  }
  return out;
}

uint64_t getMilliTick()
{
  uint64_t ticks = 0;
  struct timeval tv;
  gettimeofday(&tv, NULL);

  ticks = tv.tv_sec * 1000L + tv.tv_usec / 1000L;
  return tick;
}

void msleep(uint32_t msec)
{
  usleep(msec * 1000);
}

int64_t getFileSize(const char * path)
{
  int64_t filesize = -1;
  if (NULL == path) {
    return filesize;
  } else {
    struct stat buf;
    if (0 == stat(path, &buf)) {
      filesize = buf.st_size;
    }
    return filesize;
  }
}

void writePid()
{
  uint32_t pid = (uint32_t)getpid();
  FILE * fp = NULL;
  fp = fopen("server.pid", "w");
  assert(fp);
  char pidStr[32] = {0};
  snprintf(pidStr, sizeof(pidStr), "%d", pid);
  fwrite(pidStr, strlen(pidStr), 1, fp);
  fclose(fp);
}
