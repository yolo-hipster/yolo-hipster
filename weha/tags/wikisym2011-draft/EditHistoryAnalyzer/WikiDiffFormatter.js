function toggleExpand(name)
{
	var obj = document.getElementById(name);
	if (obj.style.display=='block')
		obj.style.display='none';
	else
		obj.style.display='block';
}