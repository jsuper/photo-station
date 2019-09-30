/**
 * 地理位置元数据
 */
export interface Location {
  nation?: string,
  province?: string,
  city?: string,
  district?: string,
  street?: string,
  address?: string
}

/**
 * 相机元数据
 */
export interface Camera {
  maker?: string,
  model?: string,
  aperture?: string,
  shutter?: string,
  focalLength?: string,
  iso?: string,
  exposure?: string,
}

export interface Photo {
  id?: string,
  title?: string,
  note?: string,
  name?: string,
  albums?: string[],
  device?: string,
  height?: number,
  width?: number
  latitude?: number,
  longitude?: number,
  path?: string,
  date?: string,
  size?: number,
  tags?: string[],
  timestamp?: number,
  type?: string,
  location?: Location,
  camera?: Camera,
  favorite?: number,
}
