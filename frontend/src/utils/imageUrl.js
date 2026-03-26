export const getImageUrl = (path) => {
  if (!path) return null;
  if (path.startsWith('http') || path.startsWith('blob') || path.startsWith('data:')) {
    return path;
  }
  
  const baseUrl = 'http://localhost:8000';
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  
  if (cleanPath.startsWith('/media/')) {
    return `${baseUrl}${cleanPath}`;
  }
  
  return `${baseUrl}/media${cleanPath}`;
};
