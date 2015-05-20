#pragma once

#define MAX_HANDS 10

#include "Vector3.h"

typedef Vector3 AccelData;

class HandsData {
public:
	AccelData hands[MAX_HANDS];
	unsigned int quantity;

	HandsData(){
		quantity = 0;
	}
};