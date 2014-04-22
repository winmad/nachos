#include "syscall.h"

int main()
{
	int i , j , k;
	int child[10] , status;
	char *name = "testConsoleOutput.coff";
	for (i = 0; i < 10; i++)
	{
		//printf("====run child %d====\n" , i);
		child[i] = exec(name , 1 , &name);
		//printf("====================\n");
	}
	for (i = 0; i < 10; i++) 
	{
		if (join(child[i] , &status) == 1) 
		{
			printf("child %d joined successfully\n" , i);
		}
	}
	return 0;
}
