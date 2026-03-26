import React, { useState, useCallback } from 'react';
import Cropper from 'react-easy-crop';
import { RotateCw, ZoomIn, ZoomOut, Check, X } from 'lucide-react';

const ImageCropper = ({ image, onCropComplete, onCancel }) => {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState(null);

  const onCropChange = (crop) => {
    setCrop(crop);
  };

  const onZoomChange = (zoom) => {
    setZoom(zoom);
  };

  const onRotate = () => {
    setRotation((prev) => (prev + 90) % 360);
  };

  const onCropCompleteInternal = useCallback((croppedArea, croppedAreaPixels) => {
    setCroppedAreaPixels(croppedAreaPixels);
  }, []);

  const createImage = (url) =>
    new Promise((resolve, reject) => {
      const image = new Image();
      image.addEventListener('load', () => resolve(image));
      image.addEventListener('error', (error) => reject(error));
      image.setAttribute('crossOrigin', 'anonymous');
      image.src = url;
    });

  const getCroppedImg = async (imageSrc, pixelCrop, rotation = 0) => {
    const image = await createImage(imageSrc);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');

    const maxSize = Math.max(image.width, image.height);
    const safeArea = 2 * ((maxSize / 2) * Math.sqrt(2));

    // Set each dimension to double largest dimension to allow for a safe area for the
    // image to rotate in without being clipped by canvas boundaries
    canvas.width = safeArea;
    canvas.height = safeArea;

    // translate canvas context to a central point of image to allow rotating around the center.
    ctx.translate(safeArea / 2, safeArea / 2);
    ctx.rotate((rotation * Math.PI) / 180);
    ctx.translate(-safeArea / 2, -safeArea / 2);

    // draw rotated image and store data.
    ctx.drawImage(
      image,
      safeArea / 2 - image.width * 0.5,
      safeArea / 2 - image.height * 0.5
    );
    const data = ctx.getImageData(0, 0, safeArea, safeArea);

    // set canvas width to final desired crop size - this will clear existing context
    canvas.width = pixelCrop.width;
    canvas.height = pixelCrop.height;

    // paste generated rotate image with correct offsets for x,y crop values.
    ctx.putImageData(
      data,
      Math.round(0 - safeArea / 2 + image.width * 0.5 - pixelCrop.x),
      Math.round(0 - safeArea / 2 + image.height * 0.5 - pixelCrop.y)
    );

    // As a blob
    return new Promise((resolve) => {
      canvas.toBlob((blob) => {
        resolve(blob);
      }, 'image/jpeg');
    });
  };

  const handleSave = async () => {
    try {
      const croppedImage = await getCroppedImg(image, croppedAreaPixels, rotation);
      onCropComplete(croppedImage);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0,0,0,0.85)', zIndex: 2000,
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      padding: '20px'
    }}>
      <div style={{
        position: 'relative', width: '100%', maxWidth: '500px', height: '400px',
        background: '#000', borderRadius: '16px', overflow: 'hidden'
      }}>
        <Cropper
          image={image}
          crop={crop}
          zoom={zoom}
          rotation={rotation}
          aspect={1}
          cropShape="round"
          showGrid={true}
          onCropChange={onCropChange}
          onCropComplete={onCropCompleteInternal}
          onZoomChange={onZoomChange}
        />
      </div>

      <div style={{
        width: '100%', maxWidth: '500px', background: 'white', 
        padding: '24px', borderRadius: '16px', marginTop: '20px',
        display: 'flex', flexDirection: 'column', gap: '20px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <ZoomOut size={18} color="var(--text-muted)" />
          <input
            type="range"
            value={zoom}
            min={1}
            max={3}
            step={0.1}
            onChange={(e) => onZoomChange(e.target.value)}
            style={{ flex: 1, accentColor: 'var(--accent-primary)' }}
          />
          <ZoomIn size={18} color="var(--text-muted)" />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button 
            type="button" 
            className="btn btn-ghost" 
            onClick={onRotate}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-primary)' }}
          >
            <RotateCw size={18} /> Rotate Image
          </button>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button 
              type="button" 
              className="btn btn-outline" 
              onClick={onCancel}
              style={{ padding: '8px 16px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <X size={18} /> Cancel
            </button>
            <button 
              type="button" 
              className="btn btn-primary" 
              onClick={handleSave}
              style={{ padding: '8px 24px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <Check size={18} /> Apply Crop
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ImageCropper;
