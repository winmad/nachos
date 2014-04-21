#include "syscall.h"

char buf[1024];

int main()
{
	int i , j;
	for (i = 0; i < 5; i++)
	{
		for (j = 0; j < 5; j++)
			printf("%d " , i + j);
		printf("\n");
	}
	return 0;
}
