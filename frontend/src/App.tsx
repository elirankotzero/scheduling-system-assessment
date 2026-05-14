import { Toaster } from 'react-hot-toast';
import { Dashboard } from './components/Dashboard';

function App() {
  return (
    <>
      <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
      <Dashboard />
    </>
  );
}

export default App;
