#include "syscall.h"

struct zmf1
{
	char wlf[1];
};

struct zmf10
{
	char wlf[10];
};

struct zmf100
{
	char wlf[100];
};

struct zmf1000
{
	char wlf[1000];
};

struct zmf10000
{
	char wlf[10000];
};

int main()
{
	//Testing copying struct objects of different sizes (so that we could test reading and writing virtual memory within one page and accrossing pages. They should all be successful.
	struct zmf1 a1, b1;
	struct zmf10 a2, b2;
	struct zmf100 a3, b3;
	struct zmf1000 a4, b4;
	struct zmf10000 a5, b5;
	int i;
	for(i = 0; i < 1; i++)
	{
		a1.wlf[i] = i;
		b1.wlf[i] = -i;
	}
	for(i = 0; i < 10; i++)
	{
		a2.wlf[i] = i;
		b2.wlf[i] = -i;
	}
	for(i = 0; i < 100; i++)
	{
		a3.wlf[i] = i;
		b3.wlf[i] = -i;
	}
	for(i = 0; i < 1000; i++)
	{
		a4.wlf[i] = i;
		b4.wlf[i] = -i;
	}
	for(i = 0; i < 10000; i++)
	{
		a5.wlf[i] = i;
		b5.wlf[i] = -i;
	}
	//Copy. Use both readvirtualmemory and writevirtualmemory.
	b1 = a1;
	b2 = a2;
	b3 = a3;
	b4 = a4;
	b5 = a5;
	//Assuring no errors, so we expect no outputs!
	for(i = 0; i < 1; i++)
		if(a1.wlf[i] != b1.wlf[i])
			printf("Error1!\n");
	for(i = 0; i < 10; i++)
		if(a2.wlf[i] != b2.wlf[i])
			printf("Error2!\n");

	for(i = 0; i < 100; i++)
		if(a3.wlf[i] != b3.wlf[i])
			printf("Error3!\n");

	for(i = 0; i < 1000; i++)
		if(a4.wlf[i] != b4.wlf[i])
			printf("Error4!\n");

	for(i = 0; i < 10000; i++)
		if(a5.wlf[i] != b5.wlf[i])
			printf("Error5!\n");
	return 0;
}
