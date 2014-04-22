#include "syscall.h"

int main()
{
	int i , j , k;
	int child[10] , status;
	char *name = "matrix-mul2.coff";
	for (i = 0; i < 10; i++)
	{
		child[i] = exec(name , 1 , &name);
		printf("run child %d\n" , i);
	}
	for (i = 0; i < 10; i++) 
	{
		if (join(child[i] , &status) != 1) 
		{
			printf("error join, status = %d\n" , status);
		}
	}
	return 0;
}