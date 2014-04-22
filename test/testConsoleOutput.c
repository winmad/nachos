#include "syscall.h"

int main()
{
	int i , j;
	
	for (i = 0; i < 5; i++)
	{
		printf("%d " , i);
	}
	printf("\n");
	close(1);
	return 0;
}
