/*
 * Copyright (C) 2014 Fastboot Mobile, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SRC_MAIN_JNI_NATIVESOCKET_SOCKETCLIENT_H_
#define SRC_MAIN_JNI_NATIVESOCKET_SOCKETCLIENT_H_

#include <string>
#include <thread>
#include "SocketCallbacks.h"
#include "SocketCommon.h"

/**
 * Client socket. This is the socket plug-ins should use to receive requests from the main app,
 * and receive/push audio data to it.
 */
class SocketClient : public SocketCommon {
 public:
    // ctor
    explicit SocketClient(const std::string& socket_name);

    // dtor
    virtual ~SocketClient();

    // Initializes the socket
    bool initialize();

 private:
    // Implements virtual parent method
    bool writeToSocket(const uint8_t* data, uint32_t len);

    // Process events (update network, calls callbacks). Called auto by the thread
    void processEventsThread();
    int processEvents();

 private:
    int32_t m_Server;
    int8_t* m_pBuffer;
    std::thread m_EventThread;
};

#endif  // SRC_MAIN_JNI_NATIVESOCKET_SOCKETCLIENT_H_
