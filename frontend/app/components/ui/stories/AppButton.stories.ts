import type { Meta, StoryObj } from '@storybook/vue3'
import AppButton from '../AppButton.vue'

const meta = {
  title: 'UI/Button',
  component: AppButton,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'outline', 'ghost', 'danger', 'success', 'warning']
    },
    size: {
      control: 'select',
      options: ['xs', 'sm', 'md', 'lg', 'xl']
    },
    disabled: {
      control: 'boolean'
    },
    loading: {
      control: 'boolean'
    },
    fullWidth: {
      control: 'boolean'
    },
    rounded: {
      control: 'boolean'
    }
  },
  args: {
    label: 'Button',
    variant: 'primary',
    size: 'md',
    disabled: false,
    loading: false,
    fullWidth: false,
    rounded: true
  }
} satisfies Meta<typeof AppButton>

export default meta
type Story = StoryObj<typeof meta>

export const Primary: Story = {
  args: {
    variant: 'primary',
    label: 'Primary Button'
  }
}

export const Secondary: Story = {
  args: {
    variant: 'secondary',
    label: 'Secondary Button'
  }
}

export const Outline: Story = {
  args: {
    variant: 'outline',
    label: 'Outline Button'
  }
}

export const Ghost: Story = {
  args: {
    variant: 'ghost',
    label: 'Ghost Button'
  }
}

export const Danger: Story = {
  args: {
    variant: 'danger',
    label: 'Danger Button'
  }
}

export const Success: Story = {
  args: {
    variant: 'success',
    label: 'Success Button'
  }
}

export const Warning: Story = {
  args: {
    variant: 'warning',
    label: 'Warning Button'
  }
}

export const Small: Story = {
  args: {
    size: 'sm',
    label: 'Small Button'
  }
}

export const Large: Story = {
  args: {
    size: 'lg',
    label: 'Large Button'
  }
}

export const WithIcon: Story = {
  args: {
    icon: 'lucide:plus',
    label: 'Add Item'
  }
}

export const Loading: Story = {
  args: {
    loading: true,
    label: 'Loading...'
  }
}

export const Disabled: Story = {
  args: {
    disabled: true,
    label: 'Disabled Button'
  }
}
