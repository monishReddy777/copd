export const getImageUrl = (path) => {
  if (!path) return null;
  if (path.startsWith('http') || path.startsWith('blob') || path.startsWith('data:')) {
    return path;
  }
  
  const baseUrl = window.location.hostname === 'localhost' ? 'http://localhost:8000' : `${window.location.protocol}//${window.location.hostname}:8000`;
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  
  // If it's already an absolute path from the server
  if (cleanPath.startsWith('/media/')) {
    return `${baseUrl}${cleanPath}`;
  }
  
  // Handle relative paths without /media/
  return `${baseUrl}/media${cleanPath}`;
};
