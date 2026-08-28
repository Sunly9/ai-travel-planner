import { createApp } from 'vue'
import 'vant/lib/index.css'
// import './style.css'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { 
    Tabbar,
     TabbarItem,
      NavBar, 
      NoticeBar,
       Field,
       Popup,
       Picker,
       Button,
       showToast,
       Grid, 
       GridItem,
       Loading,
       Empty,
       Collapse, 
       CollapseItem,
       Cell, 
       CellGroup,
       Tag
    } from 'vant';

const app = createApp(App)
app.use(createPinia())
app.use(Tabbar)
app.use(TabbarItem)
app.use(NavBar)
app.use(router)
app.use(NoticeBar)
app.use(Field)
app.use(Popup)
app.use(Picker)
app.use(Tag)
app.use(Empty)
app.use(Button)
app.use(Collapse)
app.use(CollapseItem)
app.use(Cell)
app.use(CellGroup)
app.use(Loading)
app.use(showToast)
app.use(Grid)
app.use(GridItem)
app.mount('#app')