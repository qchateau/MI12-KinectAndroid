#include "Socket.h"
#include <stdio.h>
#include <stdlib.h>


Socket::Socket(){

	print_out = fopen("out.txt", "w");
	if (print_out < 0){
		exit(-1);
	}

	printf("\nInitialising Winsock...");
	if (WSAStartup(MAKEWORD(2, 2), &WSAData) != 0)
	{
		printf("Failed. Error Code : %d", WSAGetLastError());
		//return 1;
	}
	printf("Initialised.\n");

	if ((server = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET)
	{
		printf("Could not create socket : %d", WSAGetLastError());
	}

	printf("Socket created.\n");

	//Prepare the sockaddr_in structure
	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = INADDR_ANY;
	server_addr.sin_port = htons(8888);

	//Bind
	if (bind(server, (SOCKADDR*)&server_addr, sizeof(server_addr)) == SOCKET_ERROR)	{
		printf("Bind failed with error code : %d", WSAGetLastError());
	}

	printf("Bind done");

	listen(server, 0);

	printf("Waiting for incoming connections...");

	int connection_addr_size = sizeof(connection_addr);
	connection = accept(server, (SOCKADDR*)&connection_addr, &connection_addr_size);
	if (connection == INVALID_SOCKET) {
		printf("accept failed with error code : %d", WSAGetLastError());
	}
	else{
		send(connection, "Hello world!\r\n", 14, 0);
		//closesocket(connection);
	}

	puts("Connection accepted");
}


Socket::~Socket(){


}


void Socket::pushHand(char side, int skel_id, const Vector4 &vec){
	char buffer[512];
	sprintf_s(buffer, 512, "%i;%s;%f;%f;%f\n", skel_id, side, vec.x, vec.y, vec.z);
	fprintf(print_out, "Sending:%c", buffer);
	send(connection, buffer, strlen(buffer), 0);
};