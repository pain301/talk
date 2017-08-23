#ifndef __EVENT_DISPATCHER_H__
#define __EVENT_DISPATCHER_H__

#include "types.h"
#include "util.h"

bool isRunning();

void event_dispatcher_initialize();

void startEventDispatch(uint32_t wait_timeout = 100);

void stopEventDispatch();

void addEvent(int fd, uint8_t event);

void removeEvent(int fd, uint8_t event);

void addTimer(callback_t callback, void * user_data, uint64_t interval);

void removeTimer(callback_t callback, void * user_data);

void addLoop(callback_t callback, void * user_data);

class EventDispatcher
{
private:
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
