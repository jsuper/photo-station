export class MenuItem {
  title: string;
  link: string;
  icon: string;
  constructor(title: string, link: string, icon: string) {
    this.title = title;
    this.link = link;
    this.icon = icon;
  }
}

export class MenuGroup {
  name: string;
  items: MenuItem[] = [];
}


export const MENUS: MenuGroup[] = [
  {
    name: '基本菜单', items: [
      { 'title': '照片', 'link': '/photos', 'icon': 'photo' },
      { 'title': '相册', 'link': '/albums', 'icon': 'photo_library' },
      { 'title': '日期', 'link': '/years', 'icon': 'date_range' },
      { 'title': '地点', 'link': '/places', 'icon': 'room' },
    ]
  }, {
    name: '扩展', items: [
      { 'title': '收藏', 'link': '/favorites', 'icon': 'favorite' },
      { 'title': '回收站', 'link': '/deleted', 'icon': 'delete' },
    ]
  }, {
    name: '应用', items: [
      { 'title': '设置', 'link': '/settings', 'icon': 'settings_application' },
    ]
  }
];

