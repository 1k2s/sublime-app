// Ícones das abas do dashboard, do Material Design Icons (https://pictogrammers.com/library/mdi/),
// licença Apache 2.0. Entram como SVG porque o lucide-react, a biblioteca de ícones do projeto,
// não tem esses dois desenhos, e não vale instalar outra biblioteca por causa de dois
function MdiIcon({ path, size = 18, className }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
      focusable="false"
      className={className}
    >
      <path d={path} />
    </svg>
  );
}

// mdi-monitor-dashboard
export function MonitorDashboardIcon(props) {
  return (
    <MdiIcon
      {...props}
      path="M21,16V4H3V16H21M21,2A2,2 0 0,1 23,4V16A2,2 0 0,1 21,18H14V20H16V22H8V20H10V18H3C1.89,18 1,17.1 1,16V4C1,2.89 1.89,2 3,2H21M5,6H14V11H5V6M15,6H19V8H15V6M19,9V14H15V9H19M5,12H9V14H5V12M10,12H14V14H10V12Z"
    />
  );
}

// mdi-table
export function TableIcon(props) {
  return (
    <MdiIcon
      {...props}
      path="M5,4H19A2,2 0 0,1 21,6V18A2,2 0 0,1 19,20H5A2,2 0 0,1 3,18V6A2,2 0 0,1 5,4M5,8V12H11V8H5M13,8V12H19V8H13M5,14V18H11V14H5M13,14V18H19V14H13Z"
    />
  );
}
