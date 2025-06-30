import React from "react";
import { cn } from "@/lib/utils";

type ButtonVariant = "default" | "outline" | "ghost" | "link";
type ButtonSize = "sm" | "md" | "lg";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant
    size?: "sm" | "md" | "lg";
}

export const Button: React.FC<ButtonProps> = ({
                                                  children,
                                                  className ,
                                                  variant = "default",
                                                  size = "md",
                                                  ...props
                                              }) => {
    const baseStyles =
        "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none";

    const variants: Record<ButtonVariant , string> = {
        default: "bg-blue-600 text-white hover:bg-blue-700",
        outline: "border border-gray-300 text-gray-700 hover:bg-gray-100",
        ghost: "text-gray-700 hover:bg-gray-100",
        link: "text-blue-600 underline hover:text-blue-800",
    };

    const sizes: Record<ButtonSize, string> = {
        sm: "h-8 px-3 text-sm",
        md: "h-10 px-4",
        lg: "h-12 px-6 text-lg",
    };

    return (
        <button
            className={cn(baseStyles, variants[variant], sizes[size], className)}
            {...props}
        >
            {children}
        </button>
    );
};
