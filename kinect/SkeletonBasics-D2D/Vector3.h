#pragma once
#include <cmath>

class Vector3
{
public:
	float d[3];

	inline Vector3(){
		d[0] = 0;
		d[1] = 0;
		d[2] = 0;
	}
	inline Vector3(float x, float y, float z){
		d[0] = x;
		d[1] = y;
		d[2] = z;
	}
	inline ~Vector3(){}

	inline float norm2(){
		return d[0] * d[0] + d[1] * d[1] + d[2] * d[2];
	}

	inline float norm(){
		return sqrt(norm2());
	}
};

