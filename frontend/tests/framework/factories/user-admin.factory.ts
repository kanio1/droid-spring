/**
 * User & Admin Factory
 *
 * Factory for creating test users and administrators with various configurations
 * Supports different roles, departments, statuses, and profiles
 *
 * Usage:
 * ```typescript
 * const admin = UserFactory.createAdmin()
 * const user = UserFactory.createStandardUser()
 * const bulkUsers = UserFactory.createMany(10, { role: 'user' })
 * ```
 */

export interface UserProfile {
  id?: string
  firstName: string
  lastName: string
  email: string
  username: string
  password?: string
  role: 'super-admin' | 'admin' | 'user-manager' | 'user' | 'viewer' | 'auditor'
  department: 'Engineering' | 'Product' | 'Sales' | 'Marketing' | 'HR' | 'Finance' | 'Operations'
  team?: string
  status: 'active' | 'inactive' | 'pending' | 'suspended'
  phoneNumber?: string
  location?: string
  hireDate?: string
  managerEmail?: string
  customFields?: Record<string, any>
}

export interface BulkUserOptions {
  count: number
  role?: UserProfile['role']
  department?: UserProfile['department']
  status?: UserProfile['status']
  excludeEmails?: string[]
  sequential?: boolean
}

export class UserFactory {
  private static namePool = {
    firstNames: [
      'John', 'Jane', 'Michael', 'Sarah', 'David', 'Emily', 'Robert', 'Emma',
      'William', 'Olivia', 'James', 'Ava', 'Richard', 'Sophia', 'Thomas', 'Isabella'
    ],
    lastNames: [
      'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis',
      'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson', 'Thomas'
    ],
    departments: ['Engineering', 'Product', 'Sales', 'Marketing', 'HR', 'Finance', 'Operations'] as const,
    teams: [
      'Backend', 'Frontend', 'Mobile', 'DevOps', 'QA', 'Security',
      'Product Management', 'UX Design', 'Data Science', 'Infrastructure'
    ]
  }

  private static emailDomains = ['example.com', 'test.com', 'company.com', 'corp.net']

  /**
   * Create a standard user
   */
  static createUser(overrides: Partial<UserProfile> = {}): UserProfile {
    const id = overrides.id || `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    const firstName = overrides.firstName || this.getRandomFirstName()
    const lastName = overrides.lastName || this.getRandomLastName()
    const username = overrides.username || `${firstName.toLowerCase()}.${lastName.toLowerCase()}`
    const email = overrides.email || `${username}@${this.getRandomEmailDomain()}`
    const role = overrides.role || 'user'
    const department = overrides.department || 'Engineering'
    const status = overrides.status || 'active'

    return {
      id,
      firstName,
      lastName,
      email,
      username,
      password: overrides.password || this.generatePassword(),
      role,
      department,
      team: overrides.team || this.getRandomTeam(),
      status,
      phoneNumber: overrides.phoneNumber || this.generatePhoneNumber(),
      location: overrides.location || this.getRandomLocation(),
      hireDate: overrides.hireDate || this.getRandomHireDate(),
      managerEmail: overrides.managerEmail,
      customFields: overrides.customFields || {}
    }
  }

  /**
   * Create an admin user
   */
  static createAdmin(overrides: Partial<UserProfile> = {}): UserProfile {
    return this.createUser({
      role: 'admin',
      department: 'Operations',
      ...overrides
    })
  }

  /**
   * Create a super admin
   */
  static createSuperAdmin(overrides: Partial<UserProfile> = {}): UserProfile {
    return this.createUser({
      role: 'super-admin',
      department: 'Operations',
      ...overrides
    })
  }

  /**
   * Create a user manager
   */
  static createUserManager(overrides: Partial<UserProfile> = {}): UserProfile {
    return this.createUser({
      role: 'user-manager',
      department: 'HR',
      ...overrides
    })
  }

  /**
   * Create a viewer
   */
  static createViewer(overrides: Partial<UserProfile> = {}): UserProfile {
    return this.createUser({
      role: 'viewer',
      status: 'active',
      ...overrides
    })
  }

  /**
   * Create multiple users
   */
  static createMany(count: number, options: Partial<BulkUserOptions> = {}): UserProfile[] {
    const users: UserProfile[] = []
    const { role, department, status, sequential = false } = options

    for (let i = 0; i < count; i++) {
      let user: UserProfile

      if (sequential) {
        // Create sequential users
        user = this.createUser({
          firstName: `User${i + 1}`,
          lastName: 'Test',
          email: `user${i + 1}@test.com`,
          username: `usertest${i + 1}`,
          role: role || 'user',
          department: department || 'Engineering',
          status: status || 'active'
        })
      } else {
        // Create random users
        user = this.createUser({
          role: role || 'user',
          department: department || 'Engineering',
          status: status || 'active'
        })
      }

      users.push(user)
    }

    return users
  }

  /**
   * Create a bulk of admins
   */
  static createAdmins(count: number): UserProfile[] {
    return this.createMany(count, { role: 'admin', department: 'Operations' })
  }

  /**
   * Create a bulk of standard users
   */
  static createStandardUsers(count: number): UserProfile[] {
    return this.createMany(count, { role: 'user' })
  }

  /**
   * Create a bulk of users with different roles
   */
  static createMixedRoles(count: number): UserProfile[] {
    const roles: UserProfile['role'][] = ['user', 'viewer', 'user-manager']
    const users: UserProfile[] = []

    for (let i = 0; i < count; i++) {
      const role = roles[Math.floor(Math.random() * roles.length)]
      users.push(this.createUser({ role }))
    }

    return users
  }

  /**
   * Create users from different departments
   */
  static createByDepartment(department: UserProfile['department'], count: number): UserProfile[] {
    return this.createMany(count, { department })
  }

  /**
   * Create inactive users for testing
   */
  static createInactiveUsers(count: number): UserProfile[] {
    return this.createMany(count, { status: 'inactive' })
  }

  /**
   * Create pending users
   */
  static createPendingUsers(count: number): UserProfile[] {
    return this.createMany(count, { status: 'pending' })
  }

  /**
   * Create suspended users
   */
  static createSuspendedUsers(count: number): UserProfile[] {
    return this.createMany(count, { status: 'suspended' })
  }

  /**
   * Generate unique email
   */
  static generateUniqueEmail(baseEmail: string, existingEmails: string[] = []): string {
    let email = baseEmail
    let counter = 1

    while (existingEmails.includes(email)) {
      email = baseEmail.replace('@', `+${counter}@`)
      counter++
    }

    return email
  }

  /**
   * Validate user data
   */
  static validateUser(user: UserProfile): { valid: boolean; errors: string[] } {
    const errors: string[] = []

    if (!user.firstName || user.firstName.length < 2) {
      errors.push('First name must be at least 2 characters')
    }

    if (!user.lastName || user.lastName.length < 2) {
      errors.push('Last name must be at least 2 characters')
    }

    if (!user.email || !user.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
      errors.push('Invalid email format')
    }

    if (!user.username || user.username.length < 3) {
      errors.push('Username must be at least 3 characters')
    }

    const validRoles = ['super-admin', 'admin', 'user-manager', 'user', 'viewer', 'auditor']
    if (!validRoles.includes(user.role)) {
      errors.push('Invalid role')
    }

    const validDepartments = this.namePool.departments as unknown as string[]
    if (!validDepartments.includes(user.department)) {
      errors.push('Invalid department')
    }

    const validStatuses = ['active', 'inactive', 'pending', 'suspended'] as const
    if (!validStatuses.includes(user.status as any)) {
      errors.push('Invalid status')
    }

    return {
      valid: errors.length === 0,
      errors
    }
  }

  /**
   * Convert user to CSV format
   */
  static toCSV(users: UserProfile[]): string {
    const headers = [
      'firstName',
      'lastName',
      'email',
      'username',
      'role',
      'department',
      'team',
      'status',
      'phoneNumber',
      'location'
    ]

    const rows = users.map(user => [
      user.firstName,
      user.lastName,
      user.email,
      user.username,
      user.role,
      user.department,
      user.team || '',
      user.status,
      user.phoneNumber || '',
      user.location || ''
    ])

    return [headers.join(','), ...rows.map(row => row.join(','))].join('\n')
  }

  /**
   * Parse CSV to users
   */
  static fromCSV(csv: string): UserProfile[] {
    const lines = csv.split('\n').filter(line => line.trim())
    const headers = lines[0].split(',')

    return lines.slice(1).map(line => {
      const values = line.split(',')
      const user: any = {}

      headers.forEach((header, index) => {
        user[header] = values[index]
      })

      return this.createUser({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        username: user.username,
        role: user.role as UserProfile['role'],
        department: user.department as UserProfile['department'],
        team: user.team,
        status: user.status as UserProfile['status'],
        phoneNumber: user.phoneNumber,
        location: user.location
      })
    })
  }

  // Private helper methods

  private static getRandomFirstName(): string {
    return this.namePool.firstNames[Math.floor(Math.random() * this.namePool.firstNames.length)]
  }

  private static getRandomLastName(): string {
    return this.namePool.lastNames[Math.floor(Math.random() * this.namePool.lastNames.length)]
  }

  private static getRandomEmailDomain(): string {
    return this.emailDomains[Math.floor(Math.random() * this.emailDomains.length)]
  }

  private static getRandomTeam(): string {
    return this.namePool.teams[Math.floor(Math.random() * this.namePool.teams.length)]
  }

  private static generatePassword(): string {
    const chars = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'
    let password = ''
    for (let i = 0; i < 12; i++) {
      password += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return password
  }

  private static generatePhoneNumber(): string {
    const prefix = ['+1', '+44', '+33', '+49', '+81'][Math.floor(Math.random() * 5)]
    const number = Math.floor(Math.random() * 10000000000).toString().padStart(10, '0')
    return `${prefix} ${number.slice(0, 3)}-${number.slice(3, 6)}-${number.slice(6)}`
  }

  private static getRandomLocation(): string {
    const locations = ['New York', 'London', 'Paris', 'Berlin', 'Tokyo', 'San Francisco', 'Toronto', 'Sydney']
    return locations[Math.floor(Math.random() * locations.length)]
  }

  private static getRandomHireDate(): string {
    const start = new Date(2020, 0, 1)
    const end = new Date()
    const date = new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()))
    return date.toISOString().split('T')[0]
  }
}

// Predefined user profiles
export const UserProfiles = {
  standardUser: (): UserProfile => UserFactory.createUser(),
  admin: (): UserProfile => UserFactory.createAdmin(),
  superAdmin: (): UserProfile => UserFactory.createSuperAdmin(),
  userManager: (): UserProfile => UserFactory.createUserManager(),
  viewer: (): UserProfile => UserFactory.createViewer(),

  engineeringTeam: (count: number): UserProfile[] => UserFactory.createByDepartment('Engineering', count),
  salesTeam: (count: number): UserProfile[] => UserFactory.createByDepartment('Sales', count),
  marketingTeam: (count: number): UserProfile[] => UserFactory.createByDepartment('Marketing', count),

  inactiveUsers: (count: number): UserProfile[] => UserFactory.createInactiveUsers(count),
  pendingUsers: (count: number): UserProfile[] => UserFactory.createPendingUsers(count),
  suspendedUsers: (count: number): UserProfile[] => UserFactory.createSuspendedUsers(count),

  bulkUsers: (count: number): UserProfile[] => UserFactory.createMany(count),
  bulkAdmins: (count: number): UserProfile[] => UserFactory.createAdmins(count),
  mixedRoles: (count: number): UserProfile[] => UserFactory.createMixedRoles(count)
}
