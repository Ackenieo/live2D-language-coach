import { BarChart3, Clock3, House, UserRound } from 'lucide-react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';

const navItems = [
  { to: '/history', label: '历史聊天得分', icon: Clock3 },
  { to: '/leaderboard', label: '排行榜', icon: BarChart3 },
  { to: '/profile', label: '用户管理', icon: UserRound },
];

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();

  function handleTransitionNavigate(event: React.MouseEvent<HTMLAnchorElement>, to: string) {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.altKey || event.ctrlKey || event.shiftKey) return;
    if (location.pathname === to) return;

    event.preventDefault();
    if (document.startViewTransition) {
      document.startViewTransition(() => navigate(to));
      return;
    }
    navigate(to);
  }

  return (
    <header className="top-nav" aria-label="主导航">
      <Link className="nav-home" to="/welcome" aria-label="回到欢迎页面">
        <House size={18} aria-hidden="true" />
        <span>首页</span>
      </Link>

      <nav className="nav-actions" aria-label="功能导航">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              className={({ isActive }) => `nav-icon-button${isActive ? ' active' : ''}`}
              to={item.to}
              title={item.label}
              aria-label={item.label}
              onClick={(event) => handleTransitionNavigate(event, item.to)}
            >
              <Icon size={20} aria-hidden="true" />
            </NavLink>
          );
        })}
      </nav>
    </header>
  );
}
