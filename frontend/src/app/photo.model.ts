export interface Location {
  nation?: string,
  province?: string,
  city?: string,
  district?: string,
  street?: string,
  address?: string
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
  locationInfo?: Location
}
