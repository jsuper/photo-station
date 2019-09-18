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
  aperture?: string,
  shutter?: string,
  hyperfocal?: string,
  iso: string,
}

export interface Photo {
  id?: string,
  title?: string,
  note?: string,
  name?: string,
  album?: string[],
  device?: string,
  height?: number,
  width?: number
  latitude?: number,
  longitude?: number,
  path?: string,
  shootingDate?: string,
  size?: number,
  tags?: string[],
  timestamp?: number,
  type?: string,
  locationInfo?: Location,
  camera?: Camera,
}
