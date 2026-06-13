const base = {
    get() {
        // 根据当前页面 URL 动态推导后端地址, 不再硬编码 localhost
        // 部署在 nginx 反代 /zonghexiaoqiguanlixitong 下时自动适配
        const origin = window.location.origin;
        const ctxPath = '/zonghexiaoqiguanlixitong';
        return {
            url : origin + ctxPath + "/",
            name: "zonghexiaoqiguanlixitong",
            // 退出到首页链接
            indexUrl: origin + ctxPath + '/front/index.html'
        };
    },
    getProjectName(){
        return {
            projectName: "综合小区管理系统"
        }
    }
}
export default base
