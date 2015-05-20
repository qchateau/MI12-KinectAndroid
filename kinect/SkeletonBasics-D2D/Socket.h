#pragma once

#include "stdafx.h"
#include <strsafe.h>
#include "resource.h"
#include "NuiApi.h"
#include "Data.h"
#include <WinSock2.h>
#pragma comment(lib, "ws2_32.lib")

class Socket
{
	WSADATA WSAData;
	SOCKET server;
	SOCKET connection;
	SOCKADDR_IN server_addr;
	SOCKADDR_IN connection_addr;
	FILE *print_out;
public:
	Socket();
	~Socket();
	void pushHand(char side, int skel_id, const Vector4 &vec);

};

