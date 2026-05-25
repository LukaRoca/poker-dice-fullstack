import { createRoot } from 'react-dom/client'
import './styles/index.css'
import { App } from './App.tsx'
import {SSEEmitterProvider} from "./providers/SSEContext.tsx";

const root = createRoot(document.getElementById('main-div') as HTMLElement)
root.render(
    <SSEEmitterProvider>
        <App />
    </SSEEmitterProvider>
)
