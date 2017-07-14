#ifndef __EVENT_DISPATCHER_H__
#define __EVENT_DISPATCHER_H__

#include "types.h"
#include "util.h"

class EventDispatcher
{
public:
  virtual ~EventDispatcher();

  EventDispatcher * getInstance();

  bool isRunning() {return running};
  void startEventDispatch(uint32_t wait_timeout = 100);
  void stopEventDispatch();

  void addEvent(SOCKET fd, uint8_t event);
  void removeEvent(SOCKET fd, uint8_t event);

  void addTimer(callback_t callback, void * user_data, uint64_t interval);
  void removeTimer(callback_t callback, void * user_data);

  void addLoop(callback_t callback, void * user_data);
protected:
  EventDispatcher();
private:
  static EventDispatcher * instance;

  int m_epfd;
  volatile bool running;

  typedef struct {
    callback_t callback;
    void * user_data;
    uint64_t interval;
    uint64_t next_tick;
  } TimerItem;

  CLock m_lock;
  list<TimerItem *> m_timerList;
  list<TimerItem *> m_loopList;

  void checkTimer();
  void checkLoop();
};

#endif

/*
#include "ostype.h"
#include "util.h"
#include "Lock.h"

enum {
        SOCKET_READ     = 0x1,
        SOCKET_WRITE    = 0x2,
        SOCKET_EXCEP    = 0x4,
        SOCKET_ALL      = 0x7
};

