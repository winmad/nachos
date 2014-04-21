#include "syscall.h"
#include "stdio.h"

int main()
{
	char *name1 = "matrix-mul.coff", *name2 = "matrix-mul2.coff", *name3 = "matrix-mul3.coff", *name4 = "test_exit.coff";
	int child1, child2, child3, child4, i, ret1 = -2, ret2 = -2, ret3 = -2;
	child1 = exec(name1, 1, &name1);
	int status;
	if(child1 >= 0)
		ret1 = join(child1 , &status);
	if(ret1 == 1)
		printf("Join successfully!\n");
	child2 = exec(name2, 1, &name2);
	for(i = 0; i < 10; i++)
		printf("233");
	if(child2 >= 0)
		ret2 = join(child2 , &status);
	if(ret2 == -1)
		printf("Failed gracefully!\n");
	child3 = exec(name3, 1, &name3);
	if(child3 >= 0)
		ret3 = join(child3 , &status);
	if(ret3 == 0)
		printf("Exception returns successfully!\n");
	child1 = exec(name1, 1, &name1);
	child3 = exec(name3, 1, &name3);
	if(child1 >= 0 && child3 >= 0 && child1 != child3)
		printf("Different pids!\n");
	child4 = exec(name4, 1, &name4);	//we should see only one line of "23333.." stuff!
}

